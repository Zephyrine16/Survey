package com.example.survey.security;

import com.example.survey.config.RateLimitProperties;
import com.example.survey.config.SurveyProperties;
import com.example.survey.validation.TextResponseLengthValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RequestHardeningTest {
    @Test
    void concurrentSubmissionsAllowOnlyOneRequestPerIp() throws Exception {
        var properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setPath("/submit-category");
        properties.setWindowSeconds(15L);
        properties.setMaxSize(100L);
        properties.setMessage("Please wait");
        var state = org.mockito.Mockito.mock(SecurityStateStore.class);
        var calls = new AtomicInteger();
        org.mockito.Mockito.when(state.consume("survey", "203.0.113.1", 1, 15L))
                .thenAnswer(invocation -> calls.incrementAndGet() == 1);
        var filter = new RateLimitFilter(properties, new ClientIpResolver(false), state);
        var allowed = new AtomicInteger();
        var blocked = new AtomicInteger();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(20)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Callable<Void>>();
            for (int i = 0; i < 20; i++) tasks.add(() -> {
                var request = new MockHttpServletRequest("POST", "/submit-category");
                request.setRemoteAddr("203.0.113.1");
                var response = new MockHttpServletResponse();
                filter.doFilter(request, response, (req, res) -> allowed.incrementAndGet());
                if (response.getStatus() == 429) {
                    blocked.incrementAndGet();
                    assertEquals("15", response.getHeader("Retry-After"));
                }
                return null;
            });
            for (var future : pool.invokeAll(tasks)) future.get();
        }
        assertEquals(1, allowed.get());
        assertEquals(19, blocked.get());
        var preflight = new MockHttpServletRequest("OPTIONS", "/submit-category");
        filter.doFilter(preflight, new MockHttpServletResponse(), (req, res) -> allowed.incrementAndGet());
        assertEquals(2, allowed.get());
    }

    @Test
    void rejectsLargeJsonEvenWithoutContentLength() throws Exception {
        var request = new MockHttpServletRequest("POST", "/submit-category") {
            @Override public long getContentLengthLong() { return -1; }
            @Override public int getContentLength() { return -1; }
        };
        request.setContentType("application/json");
        request.setContent(new byte[1024 * 1024 + 1]);
        var response = new MockHttpServletResponse();
        new JsonBodyLimitFilter().doFilter(request, response, (req, res) -> fail("Oversized body reached controller"));
        assertEquals(413, response.getStatus());
    }

    @Test
    void loginHasSmallerBodyLimitAndValidJsonIsPreserved() throws Exception {
        var filter = new JsonBodyLimitFilter();
        var request = new MockHttpServletRequest("POST", "/api/admin/login");
        request.setServletPath("/api/admin/login");
        request.setContentType("application/json");
        request.setContent(new byte[16 * 1024 + 1]);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Oversized login reached controller"));
        assertEquals(413, response.getStatus());
        var valid = new MockHttpServletRequest("POST", "/submit-category");
        valid.setContentType("application/json");
        byte[] body = "{\"text\":\"café\"}".getBytes(StandardCharsets.UTF_8);
        valid.setContent(body);
        filter.doFilter(valid, new MockHttpServletResponse(),
                (req, res) -> assertArrayEquals(body, req.getInputStream().readAllBytes()));
    }

    @Test
    void whitespaceCannotBypassTextLengthValidation() {
        var properties = new SurveyProperties();
        properties.setTextResponseMaxLength(250);
        var validator = new TextResponseLengthValidator(properties);
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("A short answer", null));
        assertFalse(validator.isValid(" ".repeat(251), null));
    }

    @Test
    void corsAcceptsExactOriginsAndRejectsPatterns() {
        var config = new SecurityConfig(null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://admin.example.com,http://localhost:5173");
        var source = config.corsConfigurationSource();
        var cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/menu-items"));
        assertNotNull(cors);
        assertEquals("https://admin.example.com", cors.checkOrigin("https://admin.example.com"));
        assertNull(cors.checkOrigin("https://evil.example.com"));
        for (String invalid : new String[]{"https://*.example.com", "https://example.com/path", "https://user@example.com", "https://example.com?x=1"}) {
            ReflectionTestUtils.setField(config, "allowedOrigins", invalid);
            assertThrows(IllegalStateException.class, config::corsConfigurationSource);
        }
    }
}
