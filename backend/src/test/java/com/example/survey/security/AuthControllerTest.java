package com.example.survey.security;

import com.example.survey.config.AdminLoginProperties;
import com.example.survey.dto.AdminLoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private JwtUtil jwtUtil;
    private ClientIpResolver clientIpResolver;
    private PasswordEncoder passwordEncoder;
    private AdminLoginProperties adminLoginProperties;
    private AuthController controller;

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_HASH = "$2a$10$realHashForTesting12345678901234567890";

    @BeforeEach
    void setUp() {
        jwtUtil = Mockito.mock(JwtUtil.class);
        clientIpResolver = Mockito.mock(ClientIpResolver.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);

        adminLoginProperties = new AdminLoginProperties();
        adminLoginProperties.setMaxFailedAttempts(5);
        adminLoginProperties.setLockoutMinutes(15);
        adminLoginProperties.setCacheMaxSize(100);

        controller = new AuthController(jwtUtil, clientIpResolver, passwordEncoder, adminLoginProperties);
        ReflectionTestUtils.setField(controller, "adminUser", ADMIN_USER);
        ReflectionTestUtils.setField(controller, "adminPassHash", ADMIN_HASH);
        controller.initCache();
    }

    @Test
    void testSuccessfulLogin() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.50");
        when(passwordEncoder.matches("correctPassword", ADMIN_HASH)).thenReturn(true);
        when(jwtUtil.generateToken(ADMIN_USER)).thenReturn("mock.jwt.token");

        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin");
        req.setPassword("correctPassword");

        ResponseEntity<?> response = controller.login(req, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("mock.jwt.token", ((Map<?, ?>) response.getBody()).get("token"));
    }

    @Test
    void testFailedLoginReturns401() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.50");
        when(passwordEncoder.matches("wrongPassword", ADMIN_HASH)).thenReturn(false);

        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin");
        req.setPassword("wrongPassword");

        ResponseEntity<?> response = controller.login(req, request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testFailedLoginWithWrongUserReturns401AndRunsDummyHash() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.50");
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("unknown_hacker");
        req.setPassword("password123");

        ResponseEntity<?> response = controller.login(req, request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testLockoutAfterMaxFailedAttempts() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.99");
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername("admin");
        req.setPassword("wrong");

        // 5 failed attempts
        for (int i = 0; i < 5; i++) {
            ResponseEntity<?> res = controller.login(req, request);
            assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        }

        // 6th attempt should be blocked with 429
        ResponseEntity<?> blockedResponse = controller.login(req, request);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blockedResponse.getStatusCode());
        assertTrue(((Map<?, ?>) blockedResponse.getBody()).get("error").toString().contains("Too many failed login attempts"));
    }
}
