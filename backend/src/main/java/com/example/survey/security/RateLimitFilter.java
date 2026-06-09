package com.example.survey.security;

import com.example.survey.config.RateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@NullMarked
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitProperties rateLimitProperties;
    private final Cache<String, Boolean> ipCooldowns;

    public RateLimitFilter(RateLimitProperties rateLimitProperties) {
        this.rateLimitProperties = rateLimitProperties;

        this.ipCooldowns = Caffeine.newBuilder()
                .expireAfterWrite(rateLimitProperties.getWindowSeconds(), TimeUnit.SECONDS)
                .maximumSize(rateLimitProperties.getMaxSize())
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if(!Boolean.TRUE.equals(rateLimitProperties.getEnabled())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String rateLimitPath = rateLimitProperties.getPath();

        // We only want to rate-limit the public category submission endpoint!
        if (rateLimitPath != null && !rateLimitPath.isBlank() && path.startsWith(rateLimitPath)) {
            String clientIp = extractClientIp(request);

            // Check if this IP is on cooldown
            if(ipCooldowns.getIfPresent(clientIp) != null) {
                log.warn("Blocked repeat submit-category request from IP {}", clientIp);
                response.setStatus(429); // HTTP 429 = "Too Many Requests"
                response.getWriter().write(rateLimitProperties.getMessage());
                return;
            }

            // Put them in the cache.
            // Caffeine will automatically delete this entire entry after the configured window.
            ipCooldowns.put(clientIp, Boolean.TRUE);
        }

        // Allow the request to pass through normally
        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if(forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}