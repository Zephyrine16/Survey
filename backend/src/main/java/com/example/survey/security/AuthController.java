package com.example.survey.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*") // <-- Added this to prevent CORS errors with Vue!
@RestController
@RequestMapping("/api/admin")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${admin.username}")
    private String adminUser;

    @Value("${admin.password}")
    private String adminPass;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // --- THE SNEAKY DEBUGGER ---
        System.out.println("=== NEW LOGIN ATTEMPT ===");
        System.out.println("1. What Vue sent us: [" + username + "]");
        System.out.println("2. What Render saved as the true username: [" + adminUser + "]");
        System.out.println("3. Do the usernames match? " + adminUser.equals(username));
        System.out.println("4. Do the passwords match? " + adminPass.equals(password));
        System.out.println("=========================");

        if(adminUser.equals(username) && adminPass.equals(password)) {
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
    }
}