package com.example.survey.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ClientIpResolverTest {
    private ClientIpResolver trusted() {
        var resolver = new ClientIpResolver(true);
        ReflectionTestUtils.setField(resolver, "trustedProxyAddresses", "10.0.0.1,10.0.0.2");
        resolver.validateProxyConfiguration();
        return resolver;
    }

    @Test
    void untrustedPeerCannotSupplyClientIpHeaders() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.1");
        request.addHeader("X-Forwarded-For", "203.0.113.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.2");
        request.addHeader("X-Real-IP", "203.0.113.3");
        assertEquals("198.51.100.1", trusted().resolveClientIp(request));
        assertEquals("198.51.100.1", new ClientIpResolver(false).resolveClientIp(request));
    }

    @Test
    void ignoresForgedPrefixAndCloudflareHeaderBehindVerifiedProxy() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.50");
        request.addHeader("X-Real-IP", "203.0.113.51");
        request.addHeader("X-Forwarded-For", "203.0.113.99, 198.51.100.7, 10.0.0.2");
        assertEquals("198.51.100.7", trusted().resolveClientIp(request));
    }

    @Test
    void rejectsMalformedForwardedAddressAndInvalidIpv6() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.1, ::::");
        assertEquals("10.0.0.1", trusted().resolveClientIp(request));
        assertFalse(trusted().isValidIp("::::"));
        assertTrue(trusted().isValidIp("2001:db8::1"));
    }

    @Test
    void enablingTrustWithoutAllowlistFailsAtStartup() {
        assertThrows(IllegalStateException.class, new ClientIpResolver(true)::validateProxyConfiguration);
        var resolver = new ClientIpResolver(true);
        ReflectionTestUtils.setField(resolver, "trustedProxyAddresses", "0.0.0.0/0");
        assertThrows(IllegalStateException.class, resolver::validateProxyConfiguration);
    }

    @Test
    void nullRequestReturnsDefault() {
        assertEquals("0.0.0.0", new ClientIpResolver(false).resolveClientIp(null));
    }
}
