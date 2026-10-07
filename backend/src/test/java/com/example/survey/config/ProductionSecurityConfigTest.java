package com.example.survey.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class ProductionSecurityConfigTest {
    private MockEnvironment safe() {
        return new MockEnvironment()
                .withProperty("spring.datasource.hikari.data-source-properties.sslmode", "verify-full")
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("survey.participant-cookie-secure", "true")
                .withProperty("server.forward-headers-strategy", "none")
                .withProperty("security.trusted-proxies.enabled", "true");
    }

    @Test
    void acceptsSafeProductionSettings() {
        assertDoesNotThrow(new ProductionSecurityConfig(safe())::validate);
    }

    @Test
    void rejectsUnsafeProductionOverrides() {
        for (var setting : new String[][] {
                {"spring.datasource.hikari.data-source-properties.sslmode", "disable"},
                {"spring.datasource.hikari.data-source-properties.sslmode", "require"},
                {"spring.jpa.hibernate.ddl-auto", "update"},
                {"survey.participant-cookie-secure", "false"},
                {"server.forward-headers-strategy", "framework"},
                {"security.trusted-proxies.enabled", "false"}
        }) {
            assertThrows(IllegalStateException.class,
                    new ProductionSecurityConfig(safe().withProperty(setting[0], setting[1]))::validate);
        }
        assertThrows(IllegalStateException.class, new ProductionSecurityConfig(safe().withProperty(
                "spring.datasource.url", "jdbc:postgresql://database.example.com/survey?sslmode=disable"))::validate);
    }
}
