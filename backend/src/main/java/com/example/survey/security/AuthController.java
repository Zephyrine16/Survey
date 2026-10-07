package com.example.survey.security;

import com.example.survey.config.AdminLoginProperties;
import com.example.survey.dto.AdminLoginRequest;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final ClientIpResolver clientIpResolver;

    @Value("${admin.username}")
    private String adminUser;

    @Value("${admin.password-hash}")
    private String adminPassHash;

    private final PasswordEncoder passwordEncoder;
    private final AdminLoginProperties adminLoginProperties;

    private Cache<String, AtomicInteger> failedLoginAttempts;

    @PostConstruct
    public void initCache() {
        failedLoginAttempts = Caffeine.newBuilder()
                .expireAfterWrite(adminLoginProperties.getLockoutMinutes(), TimeUnit.MINUTES)
                .maximumSize(adminLoginProperties.getCacheMaxSize())
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest credentials, HttpServletRequest request) {
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        String rateLimitKey = clientIpResolver.resolveClientIp(request);

        // Reserve an attempt atomically before the expensive password check.
        // Saturate at max + 1 so rejected traffic cannot overflow the counter.
        AtomicInteger attempts = failedLoginAttempts.get(rateLimitKey, key -> new AtomicInteger());
        int attempt = attempts.updateAndGet(count ->
                Math.min(count, adminLoginProperties.getMaxFailedAttempts()) + 1);
        if(attempt > adminLoginProperties.getMaxFailedAttempts()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many failed login attempts. Try again later."));
        }

        boolean usernameMatches = MessageDigest.isEqual(
                adminUser.getBytes(StandardCharsets.UTF_8),
                username.getBytes(StandardCharsets.UTF_8)
        );

        // Use the same configured hash and work factor for every username.
        // BCrypt accepts at most 72 UTF-8 bytes; reject longer inputs explicitly.
        boolean passwordMatches = password.getBytes(StandardCharsets.UTF_8).length <= 72
                && passwordEncoder.matches(password, adminPassHash);

        if(usernameMatches && passwordMatches) {
            failedLoginAttempts.invalidate(rateLimitKey);
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                    .body(Map.of("token", token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
    }

    @GetMapping("/session")
    public ResponseEntity<?> session() {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(Map.of("authenticated", true));
    }
}
