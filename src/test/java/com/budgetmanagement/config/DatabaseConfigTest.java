package com.budgetmanagement.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    void testNormalizeAivenRawServiceUri() {
        String aivenUri = "mysql://avnadmin:secretPass123@mysql-test-proj.a.aivencloud.com:18452/defaultdb?ssl-mode=REQUIRED";
        DatabaseConfig.DbConnectionInfo info = DatabaseConfig.parseAndNormalize(aivenUri, "defaultUser", "defaultPass", null);

        assertNotNull(info);
        assertTrue(info.jdbcUrl.startsWith("jdbc:mysql://mysql-test-proj.a.aivencloud.com:18452/defaultdb"));
        assertTrue(info.jdbcUrl.contains("sslMode=REQUIRED"));
        assertTrue(info.jdbcUrl.contains("allowPublicKeyRetrieval=true"));
        assertTrue(info.jdbcUrl.contains("serverTimezone=UTC"));
        assertFalse(info.jdbcUrl.contains("ssl-mode"));
        assertEquals("avnadmin", info.username);
        assertEquals("secretPass123", info.password);
        assertEquals("com.mysql.cj.jdbc.Driver", info.driverClassName);
    }

    @Test
    void testNormalizeAivenJdbcUrlWithSslMode() {
        String jdbcUrl = "jdbc:mysql://mysql-test-proj.a.aivencloud.com:18452/defaultdb?sslMode=REQUIRED";
        DatabaseConfig.DbConnectionInfo info = DatabaseConfig.parseAndNormalize(jdbcUrl, "avnadmin", "secretPass", null);

        assertNotNull(info);
        assertTrue(info.jdbcUrl.startsWith("jdbc:mysql://mysql-test-proj.a.aivencloud.com:18452/defaultdb"));
        assertTrue(info.jdbcUrl.contains("sslMode=REQUIRED"));
        assertTrue(info.jdbcUrl.contains("allowPublicKeyRetrieval=true"));
        assertEquals("avnadmin", info.username);
        assertEquals("secretPass", info.password);
        assertEquals("com.mysql.cj.jdbc.Driver", info.driverClassName);
    }

    @Test
    void testH2UrlSelectsH2Driver() {
        String h2Url = "jdbc:h2:mem:budget_test;MODE=MySQL";
        DatabaseConfig.DbConnectionInfo info = DatabaseConfig.parseAndNormalize(h2Url, "sa", "", "com.mysql.cj.jdbc.Driver");

        assertEquals(h2Url, info.jdbcUrl);
        assertEquals("sa", info.username);
        assertEquals("", info.password);
        // Even if defaultDriver passed was com.mysql.cj.jdbc.Driver, H2 URL MUST select org.h2.Driver
        assertEquals("org.h2.Driver", info.driverClassName);
    }

    @Test
    void testProductionMissingUrlFailsWithClearError() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                DatabaseConfig.validateProductionUrl(null, false)
        );
        assertTrue(ex.getMessage().contains("Production database configuration failed"));
        assertTrue(ex.getMessage().contains("DB_URL"));
        assertTrue(ex.getMessage().contains("Silent fallback to in-memory H2 is strictly disabled in production"));
    }

    @Test
    void testProductionBlankUrlFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                DatabaseConfig.validateProductionUrl("   ", false)
        );
        assertTrue(ex.getMessage().contains("Production database configuration failed"));
    }

    @Test
    void testProductionH2UrlFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                DatabaseConfig.validateProductionUrl("jdbc:h2:mem:budget_management", false)
        );
        assertTrue(ex.getMessage().contains("In-memory/embedded H2 URL"));
        assertTrue(ex.getMessage().contains("is not permitted in production"));
    }

    @Test
    void testLocalOrTestProfileAllowsH2() {
        assertDoesNotThrow(() ->
                DatabaseConfig.validateProductionUrl("jdbc:h2:mem:budget_management", true)
        );
    }

    @Test
    void testResolveDatabaseUrlFallbackKeysAndCleanQuotes() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", "\"mysql://avnadmin:pass@host:1234/defaultdb\"");

        String resolved = DatabaseConfig.resolveDatabaseUrl(env, "");
        assertEquals("mysql://avnadmin:pass@host:1234/defaultdb", resolved);
    }

    @Test
    void testCleanQuotes() {
        assertEquals("jdbc:mysql://localhost:3306/db", DatabaseConfig.cleanValue("\"jdbc:mysql://localhost:3306/db\""));
        assertEquals("jdbc:mysql://localhost:3306/db", DatabaseConfig.cleanValue("'jdbc:mysql://localhost:3306/db'"));
        assertEquals("plain", DatabaseConfig.cleanValue("  plain  "));
        assertNull(DatabaseConfig.cleanValue(null));
    }
}
