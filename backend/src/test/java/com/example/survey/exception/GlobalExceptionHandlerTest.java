package com.example.survey.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testHandleMaxUploadSizeExceeded() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5000000);
        ResponseEntity<Map<String, String>> response = handler.handleMaxUploadSizeExceeded(ex);

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").contains("5MB"));
    }

    @Test
    void testHandleAuthenticationException() {
        AuthenticationException ex = new AuthenticationException("Bad token") {};
        ResponseEntity<Map<String, String>> response = handler.handleAuthenticationException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Authentication failed", response.getBody().get("error"));
    }

    @Test
    void testHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ResponseEntity<Map<String, String>> response = handler.handleAccessDeniedException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access denied", response.getBody().get("error"));
    }

    @Test
    void testHandleGenericExceptionDoesNotLeakDetails() {
        RuntimeException sensitiveException = new RuntimeException("Database password in cleartext: secret123");
        ResponseEntity<Map<String, String>> response = handler.handleGenericException(sensitiveException);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().get("error").contains("secret123"));
        assertFalse(response.getBody().get("error").contains("Database password"));
        assertNotNull(response.getBody().get("errorId"));
    }
}
