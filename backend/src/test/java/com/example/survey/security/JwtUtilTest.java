package com.example.survey.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private static final String SECRET = "test-only-signing-secret-32-bytes-long";

    @Test
    void validatesSignaturesExpiryAndRequiredClaims() {
        var jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secretString", SECRET);
        ReflectionTestUtils.setField(jwt, "expirationMs", 60000L);
        jwt.init();
        assertTrue(jwt.validateToken(jwt.generateToken("admin")));
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String noExpiry = Jwts.builder().setSubject("admin").setIssuedAt(new Date())
                .signWith(key, SignatureAlgorithm.HS256).compact();
        assertFalse(jwt.validateToken(noExpiry));
        String noSubject = Jwts.builder().setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, SignatureAlgorithm.HS256).compact();
        assertFalse(jwt.validateToken(noSubject));
        String expired = Jwts.builder().setSubject("admin").setIssuedAt(new Date(System.currentTimeMillis() - 120000))
                .setExpiration(new Date(System.currentTimeMillis() - 60000)).signWith(key, SignatureAlgorithm.HS256).compact();
        assertFalse(jwt.validateToken(expired));
        assertFalse(jwt.validateToken("forged.token.signature"));
    }
}
