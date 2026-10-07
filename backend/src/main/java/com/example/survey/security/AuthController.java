package com.example.survey.security;

import com.example.survey.config.AdminLoginProperties;
import com.example.survey.dto.AdminLoginRequest;
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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final ClientIpResolver clientIpResolver;
    private final SecurityStateStore securityStateStore;

    @Value("${admin.username}")
    private String adminUser;

    @Value("${admin.password-hash}")
    private String adminPassHash;

    private final PasswordEncoder passwordEncoder;
    private final AdminLoginProperties adminLoginProperties;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest credentials, HttpServletRequest request) {
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        String rateLimitKey = clientIpResolver.resolveClientIp(request);

        if (!securityStateStore.consume("admin-login", rateLimitKey,
                adminLoginProperties.getMaxFailedAttempts(), adminLoginProperties.getLockoutMinutes() * 60L)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", Long.toString(adminLoginProperties.getLockoutMinutes() * 60L))
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
            securityStateStore.reset("admin-login", rateLimitKey);
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

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        securityStateStore.revoke(token, jwtUtil.extractExpiration(token));
        return ResponseEntity.noContent().cacheControl(org.springframework.http.CacheControl.noStore()).build();
    }
}
