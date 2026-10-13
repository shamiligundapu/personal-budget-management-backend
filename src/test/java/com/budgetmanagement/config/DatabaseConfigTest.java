package com.budgetmanagement.config;

import org.junit.jupiter.api.Test;

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
    }

    @Test
    void testH2UrlUnmodified() {
        String h2Url = "jdbc:h2:mem:budget_test;MODE=MySQL";
        DatabaseConfig.DbConnectionInfo info = DatabaseConfig.parseAndNormalize(h2Url, "sa", "", "org.h2.Driver");

        assertEquals(h2Url, info.jdbcUrl);
        assertEquals("sa", info.username);
        assertEquals("", info.password);
        assertEquals("org.h2.Driver", info.driverClassName);
    }
}
