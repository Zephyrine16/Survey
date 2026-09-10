package com.example.survey.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ClientIpResolverTest {

    @Test
    void testUntrustedProxiesIgnoresForwardedHeaders() {
        ClientIpResolver resolver = new ClientIpResolver(false);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195");
        when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.100");
        when(request.getRemoteAddr()).thenReturn("198.51.100.1");

        String resolved = resolver.resolveClientIp(request);
        assertEquals("198.51.100.1", resolved);
    }

    @Test
    void testTrustedProxiesUsesCfConnectingIp() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.50");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String resolved = resolver.resolveClientIp(request);
        assertEquals("203.0.113.50", resolved);
    }

    @Test
    void testTrustedProxiesUsesFirstValidForwardedFor() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18, 150.172.238.178");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String resolved = resolver.resolveClientIp(request);
        assertEquals("203.0.113.195", resolved);
    }

    @Test
    void testTrustedProxiesRejectsInvalidIpInForwardedFor() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        when(request.getHeader("X-Forwarded-For")).thenReturn("<script>alert(1)</script>, 203.0.113.10");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String resolved = resolver.resolveClientIp(request);
        assertEquals("203.0.113.10", resolved);
    }

    @Test
    void testNullRequestReturnsDefault() {
        ClientIpResolver resolver = new ClientIpResolver(false);
        assertEquals("0.0.0.0", resolver.resolveClientIp(null));
    }
}
