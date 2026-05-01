package com.example.survey.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // 1. This automatically deletes entries 15 seconds after they are added!
    private final Cache<String, Boolean> ipCooldowns = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS) // 15-second cooldown
            .maximumSize(10000) // Never store more than 10,000 IPs at once
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 2. We only want to rate-limit the public survey submission endpoints!
        if (path.startsWith("/submit-survey") || path.startsWith("/submit-category")) {
            String clientIp = request.getRemoteAddr();

            // 3. Check if this IP is on cooldown
            if(ipCooldowns.getIfPresent(clientIp) != null) {
                System.out.println("Blocked spam attempt from IP: " + clientIp);
                response.setStatus(429); // HTTP 429 = "Too Many Requests"
                response.getWriter().write("Please wait a few seconds before submitting again.");
                return;
            }

            // 4. Put them in the cache.
            // We just use 'Boolean.TRUE' as a dummy value because we only care about the Key (the IP address).
            // Caffeine will automatically delete this entire entry in exactly 15 seconds.
            ipCooldowns.put(clientIp, Boolean.TRUE);
        }

        // Allow the request to pass through normally
        filterChain.doFilter(request, response);
    }
}