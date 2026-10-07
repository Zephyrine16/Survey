package com.example.survey.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.expiration-ms}")
    private Long expirationMs;

    private Key key;

    // Build the secure key AFTER Spring injects the secretString
    @PostConstruct
    public void init() {
        if(secretString == null || secretString.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured.");
        }

        byte[] secretBytes = secretString.trim().getBytes(StandardCharsets.UTF_8);
        if(secretBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256.");
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);

        if(expirationMs == null || expirationMs <= 0 || expirationMs > 900000) {
            throw new IllegalStateException("JWT expiration must be between 1 and 900000 milliseconds (15 minutes).");
        }
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setId(java.util.UUID.randomUUID().toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            extractUsername(token);
            return true;
        } catch(Exception e) {
            log.debug("Invalid JWT token received.", e);
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public java.time.Instant extractExpiration(String token) {
        return extractClaims(token).getExpiration().toInstant();
    }

    private Claims extractClaims(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        if (claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getId() == null || claims.getId().isBlank()
                || claims.getExpiration() == null || claims.getIssuedAt() == null
                || !claims.getExpiration().after(claims.getIssuedAt())
                || claims.getExpiration().getTime() - claims.getIssuedAt().getTime() > 900000) {
            throw new io.jsonwebtoken.JwtException("Invalid token claims");
        }
        return claims;
    }
}
