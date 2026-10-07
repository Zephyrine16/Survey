package com.example.survey.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Refuse unsafe overrides when running the production profile. */
@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProductionSecurityConfig {
    private final Environment environment;

    @PostConstruct
    public void validate() {
        require("verify-full".equals(environment.getProperty("spring.datasource.hikari.data-source-properties.sslmode")),
                "Production database connections require DB_SSL_MODE=verify-full and a trusted server CA");
        String url = environment.getProperty("spring.datasource.url", "");
        int queryIndex = url.indexOf('?');
        if (queryIndex >= 0) {
            for (String parameter : url.substring(queryIndex + 1).split("&")) {
                String[] parts = parameter.split("=", 2);
                String name = java.net.URLDecoder.decode(parts[0], java.nio.charset.StandardCharsets.UTF_8);
                if ("sslmode".equalsIgnoreCase(name)) {
                    require(parts.length == 2 && "verify-full".equals(java.net.URLDecoder.decode(
                                    parts[1], java.nio.charset.StandardCharsets.UTF_8)),
                            "DATABASE_URL must not override certificate verification with a weaker sslmode");
                }
            }
        }
        require("validate".equals(environment.getProperty("spring.jpa.hibernate.ddl-auto")),
                "Production requires Hibernate validation with Flyway migrations");
        require(Boolean.parseBoolean(environment.getProperty("survey.participant-cookie-secure")),
                "Production requires SURVEY_COOKIE_SECURE=true");
        require("none".equals(environment.getProperty("server.forward-headers-strategy")),
                "Use the explicit proxy allowlist instead of unconditionally trusting forwarded headers");
        require(Boolean.parseBoolean(environment.getProperty("security.trusted-proxies.enabled")),
                "Production behind a reverse proxy requires SECURITY_TRUSTED_PROXIES_ENABLED=true");

        String runtimeUser = environment.getProperty("spring.datasource.username");
        String migrationUser = environment.getProperty("spring.flyway.user");
        if (runtimeUser != null || migrationUser != null) {
            require(runtimeUser != null && migrationUser != null && !runtimeUser.isBlank()
                            && !migrationUser.isBlank() && !runtimeUser.equals(migrationUser),
                    "Production requires separate least-privilege runtime and Flyway database roles");
        }
        String seedersEnabled = environment.getProperty("seeders.enabled");
        if (seedersEnabled != null) {
            require(!Boolean.parseBoolean(seedersEnabled),
                    "Production seeders must be disabled to protect existing application data");
        }
        String cleanDisabled = environment.getProperty("spring.flyway.clean-disabled");
        if (cleanDisabled != null) {
            require(Boolean.parseBoolean(cleanDisabled), "Flyway clean must remain disabled in production");
        }
        String maintenanceEnabled = environment.getProperty("security.maintenance.enabled");
        if (maintenanceEnabled != null) {
            require(!Boolean.parseBoolean(maintenanceEnabled),
                    "Production maintenance endpoints that can change or remove recorded data must remain disabled");
        }
        if (Boolean.parseBoolean(environment.getProperty("spring.flyway.baseline-on-migrate", "false"))) {
            require("10".equals(environment.getProperty("spring.flyway.baseline-version")),
                    "Existing production data may only be adopted at the reviewed baseline version 10");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
