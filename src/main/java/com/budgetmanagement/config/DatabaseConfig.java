package com.budgetmanagement.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String rawUrl = properties.getUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();

        if (rawUrl == null || rawUrl.isBlank()) {
            logger.warn("No database URL configured in DataSourceProperties, building default dataSource");
            return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        }

        DbConnectionInfo info = parseAndNormalize(rawUrl, username, password, properties.getDriverClassName());

        logger.info("Initializing HikariDataSource with driver: {} and target: {}",
                info.driverClassName, sanitizeForLog(info.jdbcUrl));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(info.jdbcUrl);
        if (info.username != null && !info.username.isBlank()) {
            config.setUsername(info.username);
        }
        if (info.password != null && !info.password.isBlank()) {
            config.setPassword(info.password);
        }
        if (info.driverClassName != null && !info.driverClassName.isBlank()) {
            config.setDriverClassName(info.driverClassName);
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(600000);
        config.setConnectionTimeout(20000);
        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(60000);

        return new HikariDataSource(config);
    }

    public static DbConnectionInfo parseAndNormalize(String rawUrl, String defaultUser, String defaultPass, String defaultDriver) {
        String trimmed = rawUrl.trim();

        // 1. If H2 or SQLite, return as is
        if (trimmed.startsWith("jdbc:h2:") || trimmed.startsWith("jdbc:sqlite:")) {
            return new DbConnectionInfo(trimmed, defaultUser, defaultPass, defaultDriver);
        }

        // 2. Check if MySQL (starts with mysql:// or jdbc:mysql://)
        boolean isMysql = trimmed.startsWith("mysql://") || trimmed.startsWith("jdbc:mysql://");
        boolean isPostgres = trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://") || trimmed.startsWith("jdbc:postgresql://");

        if (isMysql) {
            return normalizeMysql(trimmed, defaultUser, defaultPass, defaultDriver);
        }

        if (isPostgres) {
            return normalizePostgres(trimmed, defaultUser, defaultPass, defaultDriver);
        }

        return new DbConnectionInfo(trimmed, defaultUser, defaultPass, defaultDriver);
    }

    private static DbConnectionInfo normalizeMysql(String rawUrl, String defaultUser, String defaultPass, String defaultDriver) {
        String extractedUser = defaultUser;
        String extractedPass = defaultPass;

        String withoutJdbc = rawUrl.startsWith("jdbc:") ? rawUrl.substring(5) : rawUrl;

        // If it starts with mysql://, we can parse URI
        if (withoutJdbc.startsWith("mysql://")) {
            try {
                URI uri = URI.create(withoutJdbc);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    if (!parts[0].isBlank()) {
                        extractedUser = parts[0];
                    }
                    if (parts.length > 1 && !parts[1].isBlank()) {
                        extractedPass = parts[1];
                    }
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                String path = uri.getPath();
                if (path == null || path.isBlank() || "/".equals(path)) {
                    path = "/defaultdb";
                }

                Map<String, String> params = parseQuery(uri.getQuery());
                normalizeMysqlParams(params);

                String queryStr = buildQuery(params);
                String jdbcUrl = "jdbc:mysql://" + host + ":" + port + path + (queryStr.isEmpty() ? "" : "?" + queryStr);
                String driver = (defaultDriver != null && !defaultDriver.isBlank()) ? defaultDriver : "com.mysql.cj.jdbc.Driver";

                return new DbConnectionInfo(jdbcUrl, extractedUser, extractedPass, driver);
            } catch (Exception ex) {
                logger.warn("Failed to parse mysql URI via java.net.URI, applying fallback string normalization: {}", ex.getMessage());
            }
        }

        // Fallback or already jdbc:mysql:// without userinfo
        String jdbcUrl = rawUrl.startsWith("jdbc:mysql://") ? rawUrl : "jdbc:" + rawUrl;

        // Clean userinfo if present in jdbc:mysql://user:pass@host
        if (jdbcUrl.contains("@")) {
            int atIndex = jdbcUrl.indexOf("@");
            int schemeIndex = jdbcUrl.indexOf("jdbc:mysql://") + "jdbc:mysql://".length();
            String creds = jdbcUrl.substring(schemeIndex, atIndex);
            String[] parts = creds.split(":", 2);
            if (!parts[0].isBlank()) {
                extractedUser = parts[0];
            }
            if (parts.length > 1 && !parts[1].isBlank()) {
                extractedPass = parts[1];
            }
            jdbcUrl = "jdbc:mysql://" + jdbcUrl.substring(atIndex + 1);
        }

        // Parse query params if present
        int qIdx = jdbcUrl.indexOf("?");
        String baseUrl = qIdx >= 0 ? jdbcUrl.substring(0, qIdx) : jdbcUrl;
        String rawQuery = qIdx >= 0 ? jdbcUrl.substring(qIdx + 1) : "";

        Map<String, String> params = parseQuery(rawQuery);
        normalizeMysqlParams(params);

        String queryStr = buildQuery(params);
        jdbcUrl = baseUrl + (queryStr.isEmpty() ? "" : "?" + queryStr);
        String driver = (defaultDriver != null && !defaultDriver.isBlank()) ? defaultDriver : "com.mysql.cj.jdbc.Driver";

        return new DbConnectionInfo(jdbcUrl, extractedUser, extractedPass, driver);
    }

    private static DbConnectionInfo normalizePostgres(String rawUrl, String defaultUser, String defaultPass, String defaultDriver) {
        String extractedUser = defaultUser;
        String extractedPass = defaultPass;

        String withoutJdbc = rawUrl.startsWith("jdbc:") ? rawUrl.substring(5) : rawUrl;
        if (withoutJdbc.startsWith("postgres://")) {
            withoutJdbc = "postgresql://" + withoutJdbc.substring("postgres://".length());
        }

        if (withoutJdbc.startsWith("postgresql://")) {
            try {
                URI uri = URI.create(withoutJdbc);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    if (extractedUser == null || extractedUser.isBlank()) {
                        extractedUser = parts[0];
                    }
                    if (parts.length > 1 && (extractedPass == null || extractedPass.isBlank())) {
                        extractedPass = parts[1];
                    }
                }
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                if (path == null || path.isBlank() || "/".equals(path)) {
                    path = "/defaultdb";
                }
                String query = uri.getQuery();
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + (query != null && !query.isBlank() ? "?" + query : "");
                String driver = (defaultDriver != null && !defaultDriver.isBlank()) ? defaultDriver : "org.postgresql.Driver";
                return new DbConnectionInfo(jdbcUrl, extractedUser, extractedPass, driver);
            } catch (Exception ex) {
                logger.warn("Failed to parse postgresql URI: {}", ex.getMessage());
            }
        }

        String jdbcUrl = rawUrl.startsWith("jdbc:postgresql://") ? rawUrl : "jdbc:" + rawUrl;
        String driver = (defaultDriver != null && !defaultDriver.isBlank()) ? defaultDriver : "org.postgresql.Driver";
        return new DbConnectionInfo(jdbcUrl, extractedUser, extractedPass, driver);
    }

    private static void normalizeMysqlParams(Map<String, String> params) {
        if (params.containsKey("ssl-mode")) {
            String val = params.remove("ssl-mode");
            params.putIfAbsent("sslMode", val);
        }
        params.putIfAbsent("sslMode", "REQUIRED");
        params.putIfAbsent("allowPublicKeyRetrieval", "true");
        params.putIfAbsent("serverTimezone", "UTC");
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return map;
        }
        for (String param : query.split("&")) {
            if (param.isBlank()) continue;
            String[] pair = param.split("=", 2);
            map.put(pair[0].trim(), pair.length > 1 ? pair[1].trim() : "");
        }
        return map;
    }

    private static String buildQuery(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getValue().isEmpty() ? e.getKey() : e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String sanitizeForLog(String url) {
        if (url == null) return "null";
        return url.replaceAll("(?i)(password|pwd|secret)=[^&]*", "$1=***");
    }

    public static class DbConnectionInfo {
        public final String jdbcUrl;
        public final String username;
        public final String password;
        public final String driverClassName;

        public DbConnectionInfo(String jdbcUrl, String username, String password, String driverClassName) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
            this.driverClassName = driverClassName;
        }
    }
}
