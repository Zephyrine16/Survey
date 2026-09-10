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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthController {

    private static final String DUMMY_BCRYPT_HASH = "$2a$10$e8kPqZ2aZJ6w1K4e7rXGVuY7w1K4e7rXGVuY7w1K4e7rXGVuY7w1K4";

    private final JwtUtil jwtUtil;
    private final ClientIpResolver clientIpResolver;

    @Value("${admin.username}")
    private String adminUser;

    @Value("${admin.password-hash}")
    private String adminPassHash;

    private final PasswordEncoder passwordEncoder;
    private final AdminLoginProperties adminLoginProperties;

    private Cache<String, Integer> failedLoginAttempts;

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

        Integer failures = failedLoginAttempts.getIfPresent(rateLimitKey);
        int failureCount = failures == null ? 0 : failures;

        if(failureCount >= adminLoginProperties.getMaxFailedAttempts()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many failed login attempts. Try again later."));
        }

        boolean usernameMatches = MessageDigest.isEqual(
                adminUser.getBytes(StandardCharsets.UTF_8),
                username.getBytes(StandardCharsets.UTF_8)
        );

        boolean passwordMatches;
        if (usernameMatches) {
            passwordMatches = passwordEncoder.matches(password, adminPassHash);
        } else {
            // Mitigate timing attack: execute dummy check so response time is indistinguishable
            passwordEncoder.matches(password, DUMMY_BCRYPT_HASH);
            passwordMatches = false;
        }

        if(usernameMatches && passwordMatches) {
            failedLoginAttempts.invalidate(rateLimitKey);
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token));
        }

        failedLoginAttempts.put(rateLimitKey, failureCount + 1);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
    }
}