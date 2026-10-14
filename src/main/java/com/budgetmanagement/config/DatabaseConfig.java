package com.budgetmanagement.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private final Environment environment;

    public DatabaseConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        boolean isLocalOrTest = isLocalOrTestProfile();

        String rawUrl = resolveDatabaseUrl(environment, properties.getUrl());
        String username = resolveUsername(environment, properties.getUsername());
        String password = resolvePassword(environment, properties.getPassword());

        // Validate production database configuration: H2 silent fallback is strictly forbidden in production
        validateProductionUrl(rawUrl, isLocalOrTest);

        if (isLocalOrTest && (rawUrl == null || rawUrl.isBlank())) {
            rawUrl = "jdbc:h2:mem:budget_test;MODE=MySQL;NON_KEYWORDS=MONTH,YEAR;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
            if (username == null || username.isBlank()) username = "sa";
            if (password == null) password = "";
        }

        DbConnectionInfo info = parseAndNormalize(rawUrl, username, password, properties.getDriverClassName());

        if (!isLocalOrTest && info.jdbcUrl != null && info.jdbcUrl.contains("aivencloud.com")) {
            if (info.password == null || info.password.isBlank()) {
                throw new IllegalStateException(
                        "Production database configuration failed: Missing database password for Aiven MySQL. " +
                        "Please set 'DB_PASSWORD' (or 'DATABASE_PASSWORD') in Railway service variables."
                );
            }
        }

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

        // Production-safe connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(600000);
        config.setConnectionTimeout(20000);
        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(60000);

        return new HikariDataSource(config);
    }

    public boolean isLocalOrTestProfile() {
        if (environment == null) return false;
        return environment.matchesProfiles("local", "test");
    }

    public static void validateProductionUrl(String rawUrl, boolean isLocalOrTest) {
        if (!isLocalOrTest) {
            if (rawUrl == null || rawUrl.isBlank()) {
                throw new IllegalStateException(
                        "Production database configuration failed: No database URL was found. " +
                        "Railway requires 'DB_URL' (or 'DATABASE_URL') to be set to your Aiven MySQL connection string: " +
                        "jdbc:mysql://<host>:<port>/<database>?sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC. " +
                        "Silent fallback to in-memory H2 is strictly disabled in production."
                );
            }

            String trimmed = rawUrl.trim().toLowerCase();
            if (trimmed.startsWith("jdbc:h2:") || trimmed.startsWith("h2:")) {
                throw new IllegalStateException(
                        "Production database configuration failed: In-memory/embedded H2 URL ('" + rawUrl + "') " +
                        "is not permitted in production. Please configure 'DB_URL' with your Aiven MySQL connection string."
                );
            }
        }
    }

    public static String resolveDatabaseUrl(Environment env, String propertiesUrl) {
        String cleaned = cleanValue(propertiesUrl);
        if (cleaned != null && !cleaned.isBlank()) {
            return cleaned;
        }

        String[] candidateKeys = {"DB_URL", "DATABASE_URL", "MYSQL_URL", "SPRING_DATASOURCE_URL"};
        for (String key : candidateKeys) {
            if (env != null) {
                String val = cleanValue(env.getProperty(key));
                if (val != null && !val.isBlank()) {
                    return val;
                }
            }
            String envVal = cleanValue(System.getenv(key));
            if (envVal != null && !envVal.isBlank()) {
                return envVal;
            }
        }
        return null;
    }

    public static String resolveUsername(Environment env, String propertiesUser) {
        String cleaned = cleanValue(propertiesUser);
        if (cleaned != null && !cleaned.isBlank() && !"root".equals(cleaned)) {
            return cleaned;
        }

        String[] candidateKeys = {"DB_USERNAME", "DATABASE_USERNAME", "MYSQL_USER", "SPRING_DATASOURCE_USERNAME"};
        for (String key : candidateKeys) {
            if (env != null) {
                String val = cleanValue(env.getProperty(key));
                if (val != null && !val.isBlank()) {
                    return val;
                }
            }
            String envVal = cleanValue(System.getenv(key));
            if (envVal != null && !envVal.isBlank()) {
                return envVal;
            }
        }
        return cleaned;
    }

    public static String resolvePassword(Environment env, String propertiesPass) {
        String cleaned = cleanValue(propertiesPass);
        if (cleaned != null && !cleaned.isBlank()) {
            return cleaned;
        }

        String[] candidateKeys = {"DB_PASSWORD", "DATABASE_PASSWORD", "MYSQL_PASSWORD", "SPRING_DATASOURCE_PASSWORD"};
        for (String key : candidateKeys) {
            if (env != null) {
                String val = cleanValue(env.getProperty(key));
                if (val != null && !val.isBlank()) {
                    return val;
                }
            }
            String envVal = cleanValue(System.getenv(key));
            if (envVal != null && !envVal.isBlank()) {
                return envVal;
            }
        }
        return cleaned;
    }

    public static String cleanValue(String val) {
        if (val == null) return null;
        String trimmed = val.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length() >= 2) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }

    public static DbConnectionInfo parseAndNormalize(String rawUrl, String defaultUser, String defaultPass, String defaultDriver) {
        String trimmed = rawUrl.trim();

        // 1. If H2 or SQLite (allowed only in local/test)
        if (trimmed.startsWith("jdbc:h2:") || trimmed.startsWith("h2:")) {
            String jdbcUrl = trimmed.startsWith("jdbc:") ? trimmed : "jdbc:" + trimmed;
            return new DbConnectionInfo(jdbcUrl, defaultUser, defaultPass, "org.h2.Driver");
        }
        if (trimmed.startsWith("jdbc:sqlite:") || trimmed.startsWith("sqlite:")) {
            String jdbcUrl = trimmed.startsWith("jdbc:") ? trimmed : "jdbc:" + trimmed;
            return new DbConnectionInfo(jdbcUrl, defaultUser, defaultPass, "org.sqlite.JDBC");
        }

        // 2. Check if MySQL (starts with mysql:// or jdbc:mysql://)
        boolean isMysql = trimmed.startsWith("mysql://") || trimmed.startsWith("jdbc:mysql://");
        boolean isPostgres = trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://") || trimmed.startsWith("jdbc:postgresql://");

        if (isMysql) {
            return normalizeMysql(trimmed, defaultUser, defaultPass);
        }

        if (isPostgres) {
            return normalizePostgres(trimmed, defaultUser, defaultPass);
        }

        // Fallback: if driver not recognized, default to MySQL driver if not local/test
        String driver = (defaultDriver != null && !defaultDriver.isBlank()) ? defaultDriver : "com.mysql.cj.jdbc.Driver";
        return new DbConnectionInfo(trimmed, defaultUser, defaultPass, driver);
    }

    private static DbConnectionInfo normalizeMysql(String rawUrl, String defaultUser, String defaultPass) {
        String extractedUser = defaultUser;
        String extractedPass = defaultPass;

        String withoutJdbc = rawUrl.startsWith("jdbc:") ? rawUrl.substring(5) : rawUrl;

        // If it starts with mysql://, parse URI components
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

                return new DbConnectionInfo(jdbcUrl, extractedUser, extractedPass, "com.mysql.cj.jdbc.Driver");
            } catch (Exception ex) {
                logger.warn("Failed to parse mysql URI via java.net.URI, applying fallback string normalization: {}", ex.getMessage());
            }
        }

        // Fallback or already jdbc:mysql://
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

        // Ensure database name is present in baseUrl
        int schemeEnd = baseUrl.indexOf("://") + 3;
        int pathSlash = baseUrl.indexOf("/", schemeEnd);
        if (pathSlash < 0 || pathSlash == baseUrl.length() - 1) {
            baseUrl = (pathSlash < 0 ? baseUrl : baseUrl.substring(0, pathSlash)) + "/defaultdb";
        }

        Map<String, String> params = parseQuery(rawQuery);
        normalizeMysqlParams(params);

        String queryStr = buildQuery(params);
        jdbcUrl = baseUrl + (queryStr.isEmpty() ? "" : "?" + queryStr);

        return new DbConnectionInfo(jdbcUrl, extractedUser, extractedPass, "com.mysql.cj.jdbc.Driver");
    }

    private static DbConnectionInfo normalizePostgres(String rawUrl, String defaultUser, String defaultPass) {
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
                return new DbConnectionInfo(jdbcUrl, extractedUser, extractedPass, "org.postgresql.Driver");
            } catch (Exception ex) {
                logger.warn("Failed to parse postgresql URI: {}", ex.getMessage());
            }
        }

        String jdbcUrl = rawUrl.startsWith("jdbc:postgresql://") ? rawUrl : "jdbc:" + rawUrl;
        return new DbConnectionInfo(jdbcUrl, extractedUser, extractedPass, "org.postgresql.Driver");
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

    public static String sanitizeForLog(String url) {
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
