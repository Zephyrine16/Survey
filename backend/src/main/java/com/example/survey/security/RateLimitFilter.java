package com.example.survey.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // This acts as a temporary memory bank: it stores an IP Address and the time they are allowed to post next
    private final Map<String, Long> ipCooldowns = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. We only want to rate-limit the public survey submission endpoints!
        if (path.startsWith("/submit-survey") || path.startsWith("/submit-category")) {
            String clientIp = request.getRemoteAddr();
            long currentTime = System.currentTimeMillis();

            // 2. Check if this IP is on cooldown
            if (ipCooldowns.containsKey(clientIp) && currentTime < ipCooldowns.get(clientIp)) {
                System.out.println("Blocked spam attempt from IP: " + clientIp);
                response.setStatus(429); // HTTP 429 = "Too Many Requests"
                response.getWriter().write("Please wait a few seconds before submitting again.");
                return; // Stop the request dead in its tracks
            }

            // 3. Put them on a 15-second cooldown (15,000 milliseconds)
            ipCooldowns.put(clientIp, currentTime + 15000);
        }

        // Allow the request to pass through normally
        filterChain.doFilter(request, response);
    }
}