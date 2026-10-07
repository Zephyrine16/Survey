package com.example.survey.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;

/** PostgreSQL owns the clock and atomic counters, shared by all API instances. */
@Component
@RequiredArgsConstructor
public class SecurityStateStore {
    private final JdbcTemplate jdbcTemplate;

    public boolean consume(String scope, String clientIp, int maximum, long windowSeconds) {
        Integer attempts = jdbcTemplate.queryForObject("""
                INSERT INTO security_rate_limits (bucket_key, attempts, expires_at)
                VALUES (?, 1, clock_timestamp() + (? * interval '1 second'))
                ON CONFLICT (bucket_key) DO UPDATE SET
                    attempts = CASE WHEN security_rate_limits.expires_at <= clock_timestamp()
                        THEN 1 ELSE LEAST(security_rate_limits.attempts, ?) + 1 END,
                    expires_at = CASE WHEN security_rate_limits.expires_at <= clock_timestamp()
                        THEN clock_timestamp() + (? * interval '1 second')
                        ELSE security_rate_limits.expires_at END
                RETURNING attempts
                """, Integer.class, digest(scope + ":" + clientIp), windowSeconds, maximum, windowSeconds);
        return attempts != null && attempts <= maximum;
    }

    public void reset(String scope, String clientIp) {
        jdbcTemplate.update("DELETE FROM security_rate_limits WHERE bucket_key = ?", digest(scope + ":" + clientIp));
    }

    public boolean isRevoked(String token) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM revoked_admin_tokens
                    WHERE token_hash = ? AND expires_at > clock_timestamp())
                """, Boolean.class, digest(token)));
    }

    public void revoke(String token, Instant expiration) {
        jdbcTemplate.update("""
                INSERT INTO revoked_admin_tokens (token_hash, expires_at) VALUES (?, ?)
                ON CONFLICT (token_hash) DO NOTHING
                """, digest(token), Timestamp.from(expiration));
    }

    @Scheduled(fixedDelay = 60000)
    public void removeExpiredState() {
        jdbcTemplate.update("DELETE FROM security_rate_limits WHERE expires_at <= clock_timestamp()");
        jdbcTemplate.update("DELETE FROM revoked_admin_tokens WHERE expires_at <= clock_timestamp()");
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
