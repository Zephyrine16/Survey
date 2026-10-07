package com.example.survey.security;

import com.example.survey.config.SurveyProperties;
import com.example.survey.dto.CategorySubmissionDTO;
import com.example.survey.service.SurveyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/** Run only against an explicitly supplied, disposable PostgreSQL database. */
@EnabledIfEnvironmentVariable(named = "SURVEY_SECURITY_TEST_DB_URL", matches = ".+")
@SpringBootTest(properties = {
        "spring.config.import=", "spring.datasource.username=survey_security_test",
        "spring.datasource.password=", "spring.datasource.hikari.data-source-properties.sslmode=disable",
        "spring.jpa.hibernate.ddl-auto=validate", "admin.username=test-admin",
        "admin.password-hash=$2a$10$realHashForTesting12345678901234567890",
        "jwt.secret=test-only-signing-secret-32-bytes-long", "jwt.expiration-ms=900000",
        "CORS_ALLOWED_ORIGINS=http://localhost:5173", "security.trusted-proxies.enabled=false",
        "keep-alive.enabled=false", "seeders.enabled=false"
})
class PostgresSecurityTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String url = System.getenv("SURVEY_SECURITY_TEST_DB_URL");
        if (!url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/survey_security_regression")) {
            throw new IllegalStateException("Security regression tests require a disposable local survey_security_regression database");
        }
        properties.add("spring.datasource.url", () -> url);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired SecurityStateStore state;
    @Autowired SurveyService survey;
    @Autowired SurveyProperties properties;
    @Autowired JwtUtil jwt;

    @BeforeEach
    void prepare() {
        jdbc.update("DELETE FROM answers");
        jdbc.update("DELETE FROM security_rate_limits");
        jdbc.update("DELETE FROM revoked_admin_tokens");
        jdbc.update("INSERT INTO menu_items (id, name) VALUES (9001, 'Test item'), (9002, 'Second item') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO questions (id, text, question_type) VALUES (9001, 'Test question', 'TEXT') ON CONFLICT DO NOTHING");
        properties.setParticipantLimit(0L);
        properties.setItemRespondentLimit(1L);
    }

    @Test
    void concurrentReplicasShareOneRateLimit() throws Exception {
        var otherReplica = new SecurityStateStore(jdbc);
        try (var pool = Executors.newFixedThreadPool(20)) {
            var tasks = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 20; i++) {
                var replica = i % 2 == 0 ? state : otherReplica;
                tasks.add(() -> replica.consume("survey", "203.0.113.1", 1, 15));
            }
            int allowed = 0;
            for (var result : pool.invokeAll(tasks)) if (result.get()) allowed++;
            assertEquals(1, allowed);
        }
    }

    @Test
    void rateWindowsExpireAndSuccessfulLoginResetsSharedState() {
        var otherReplica = new SecurityStateStore(jdbc);
        for (int i = 0; i < 5; i++) assertTrue(state.consume("admin-login", "203.0.113.1", 5, 900));
        assertFalse(otherReplica.consume("admin-login", "203.0.113.1", 5, 900));
        assertTrue(otherReplica.consume("admin-login", "203.0.113.2", 5, 900));
        state.reset("admin-login", "203.0.113.1");
        assertTrue(otherReplica.consume("admin-login", "203.0.113.1", 5, 900));
        jdbc.update("UPDATE security_rate_limits SET expires_at = clock_timestamp() - interval '1 second'");
        assertTrue(state.consume("admin-login", "203.0.113.1", 5, 900));
    }

    @Test
    void revocationSurvivesReplicaChangesAndExpiredStateIsRemoved() {
        String token = jwt.generateToken("test-admin");
        var otherReplica = new SecurityStateStore(jdbc);
        assertFalse(state.isRevoked(token));
        state.revoke(token, jwt.extractExpiration(token));
        assertTrue(otherReplica.isRevoked(token));
        assertFalse(otherReplica.isRevoked(jwt.generateToken("test-admin")));
        jdbc.update("UPDATE revoked_admin_tokens SET expires_at = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(1)));
        state.removeExpiredState();
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM revoked_admin_tokens", Integer.class));
    }

    @Test
    void simultaneousRetriesInsertOnlyOneResponse() throws Exception {
        String session = UUID.randomUUID().toString();
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 8; i++) tasks.add(() -> submit(session, 9001L));
            for (var result : pool.invokeAll(tasks)) assertTrue(result.get());
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM answers", Integer.class));
    }

    @Test
    void concurrentSubmissionsCannotExceedItemCap() throws Exception {
        assertEquals(1, compete(9001L, 9001L));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM answers", Integer.class));
    }

    @Test
    void concurrentSubmissionsCannotExceedGlobalCap() throws Exception {
        properties.setParticipantLimit(1L);
        assertEquals(1, compete(9001L, 9002L));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM answers", Integer.class));
    }

    private int compete(long first, long second) throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var results = pool.invokeAll(List.of(
                    (Callable<Boolean>) () -> submit(UUID.randomUUID().toString(), first),
                    () -> submit(UUID.randomUUID().toString(), second)));
            int saved = 0;
            for (var result : results) if (result.get()) saved++;
            return saved;
        }
    }

    private boolean submit(String session, long itemId) {
        var answer = new CategorySubmissionDTO();
        answer.setUserId(session);
        answer.setMenuItemId(itemId);
        answer.setQuestionId(9001L);
        answer.setTextResponse("Valid feedback");
        return survey.saveCompleteSurveyIfUnderLimit(List.of(answer), session, true, null, null);
    }
}
