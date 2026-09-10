package com.example.survey.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ClientIpResolver {

    @Value("${security.trusted-proxies.enabled:false}")
    private boolean trustedProxiesEnabled;

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

        if (trustedProxiesEnabled) {
            // Check Cloudflare header first
            String cfIp = request.getHeader("CF-Connecting-IP");
            if (isValidIp(cfIp)) {
                return cfIp.trim();
            }

            // Check standard reverse proxy header
            String realIp = request.getHeader("X-Real-IP");
            if (isValidIp(realIp)) {
                return realIp.trim();
            }

            // Check X-Forwarded-For
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] parts = forwarded.split(",");
                for (String part : parts) {
                    String candidate = part.trim();
                    if (isValidIp(candidate)) {
                        return candidate;
                    }
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
        return IPV4_PATTERN.matcher(trimmed).matches() || (trimmed.contains(":") && IPV6_PATTERN.matcher(trimmed).matches());
    }
}
