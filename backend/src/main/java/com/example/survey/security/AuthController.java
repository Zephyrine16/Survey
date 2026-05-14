package com.example.survey.security;

import com.example.survey.dto.AdminLoginRequest;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/admin")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${admin.username}")
    private String adminUser;

    @Value("${admin.password-hash}")
    private String adminPassHash;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final Cache<String, Integer> failedLoginAttempts = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest credentials, HttpServletRequest request) {
        String username = credentials.getUsername();
        String password = credentials.getPassword();
        String clientIp = extractClientIp(request);
        String rateLimitKey = clientIp;

        Integer failures = failedLoginAttempts.getIfPresent(rateLimitKey);
        int failureCount = failures == null ? 0 : failures;

        if(failureCount >= MAX_FAILED_ATTEMPTS) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many failed login attempts. Try again later."));
        }

        if(adminUser.equals(username) && passwordEncoder.matches(password, adminPassHash)) {
            failedLoginAttempts.invalidate(rateLimitKey);
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token));
        }

        failedLoginAttempts.put(rateLimitKey, failureCount + 1);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if(forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}