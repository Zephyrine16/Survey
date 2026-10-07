package com.example.survey.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import jakarta.annotation.PostConstruct;

import java.util.regex.Pattern;

@Component
public class ClientIpResolver {

    @Value("${security.trusted-proxies.enabled:false}")
    private boolean trustedProxiesEnabled;

    @Value("${security.trusted-proxies.addresses:}")
    private String trustedProxyAddresses = "";
    private java.util.List<IpAddressMatcher> trustedProxyMatchers = java.util.List.of();

    @PostConstruct
    public void validateProxyConfiguration() {
        if (trustedProxiesEnabled && trustedProxyAddresses.isBlank()) {
            throw new IllegalStateException("Trusted proxy mode requires explicit proxy IP addresses or CIDRs");
        }
        if (trustedProxiesEnabled) {
            var matchers = new java.util.ArrayList<IpAddressMatcher>();
            for (String address : trustedProxyAddresses.split(",")) {
                String proxy = address.trim();
                String[] parts = proxy.split("/", -1);
                if (!isValidIp(parts[0]) || (parts.length == 2 && "0".equals(parts[1]))) {
                    throw new IllegalStateException("Trusted proxies must be explicit IPs or restricted CIDRs");
                }
                matchers.add(new IpAddressMatcher(proxy));
            }
            trustedProxyMatchers = java.util.List.copyOf(matchers);
        }
    }

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$"
    );

    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^[0-9a-fA-F:]+$"
    );

    public ClientIpResolver() {
    }

    public ClientIpResolver(boolean trustedProxiesEnabled) {
        this.trustedProxiesEnabled = trustedProxiesEnabled;
    }

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        if (trustedProxiesEnabled && isTrustedProxy(request.getRemoteAddr())) {
            // Walk from the verified peer back through known proxies. Never trust
            // the leftmost entry or Cloudflare headers supplied by arbitrary clients.
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] parts = forwarded.split(",");
                if (parts.length > 32) return request.getRemoteAddr();
                for (int i = parts.length - 1; i >= 0; i--) {
                    String candidate = parts[i].trim();
                    if (!isValidIp(candidate)) return request.getRemoteAddr();
                    if (!isTrustedProxy(candidate)) return candidate;
                }
            }
        }

        String remoteAddr = request.getRemoteAddr();
        if (isValidIp(remoteAddr)) {
            return remoteAddr.trim();
        }

        return remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr.trim() : "0.0.0.0";
    }

    public boolean isValidIp(String ip) {
        if (ip == null) {
            return false;
        }
        String trimmed = ip.trim();
        if (trimmed.isEmpty() || trimmed.length() > 45) {
            return false;
        }
        if (IPV4_PATTERN.matcher(trimmed).matches()) return true;
        if (!trimmed.contains(":") || !IPV6_PATTERN.matcher(trimmed).matches()) return false;
        try {
            return java.net.InetAddress.getByName(trimmed) instanceof java.net.Inet6Address;
        } catch (java.net.UnknownHostException exception) {
            return false;
        }
    }

    private boolean isTrustedProxy(String address) {
        if (!isValidIp(address) || trustedProxyAddresses.isBlank()) return false;
        for (IpAddressMatcher proxy : trustedProxyMatchers) {
            if (proxy.matches(address)) return true;
        }
        return false;
    }
}
