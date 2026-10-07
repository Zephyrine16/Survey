package com.example.survey.security;

import com.example.survey.config.RateLimitProperties;
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

@Component
@NullMarked
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitProperties rateLimitProperties;
    private final ClientIpResolver clientIpResolver;
    private final SecurityStateStore securityStateStore;

    public RateLimitFilter(RateLimitProperties rateLimitProperties, ClientIpResolver clientIpResolver,
                           SecurityStateStore securityStateStore) {
        this.rateLimitProperties = rateLimitProperties;
        this.clientIpResolver = clientIpResolver;

        this.securityStateStore = securityStateStore;
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
        if ("POST".equals(request.getMethod()) && rateLimitPath != null && !rateLimitPath.isBlank()
                && (path.equals(rateLimitPath) || path.startsWith(rateLimitPath + "/"))) {
            String clientIp = clientIpResolver.resolveClientIp(request);

            // Check if this IP is on cooldown
            if (!securityStateStore.consume("survey", clientIp, 1, rateLimitProperties.getWindowSeconds())) {
                log.warn("Blocked repeat submit-category request from IP {}", clientIp);
                response.setStatus(429); // HTTP 429 = "Too Many Requests"
                response.setHeader("Retry-After", Long.toString(rateLimitProperties.getWindowSeconds()));
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write(rateLimitProperties.getMessage());
                return;
            }

        }

        // Allow the request to pass through normally
        filterChain.doFilter(request, response);
    }
}
