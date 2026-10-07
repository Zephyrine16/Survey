package com.example.survey.security;

import com.example.survey.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.security.web.FilterChainProxy;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MenuAuthorizationTest {
    @Test
    void menuWritesRequireValidAdminSignature() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    context, "CORS_ALLOWED_ORIGINS=https://admin.example.com", "admin.username=admin");
            var jwt = new JwtUtil();
            ReflectionTestUtils.setField(jwt, "secretString", "test-only-signing-secret-32-bytes-long");
            ReflectionTestUtils.setField(jwt, "expirationMs", 60000L);
            jwt.init();
            var properties = new RateLimitProperties();
            properties.setEnabled(false);
            properties.setWindowSeconds(1L);
            properties.setMaxSize(100L);
            context.addBeanFactoryPostProcessor(factory -> {
                factory.registerSingleton("jwtAuthFilter", new JwtAuthFilter(jwt));
                factory.registerSingleton("rateLimitFilter", new RateLimitFilter(properties, new ClientIpResolver(false)));
            });
            context.register(SecurityConfig.class);
            context.refresh();
            // Manually registered instances do not receive @Value injection.
            ReflectionTestUtils.setField(context.getBean(JwtAuthFilter.class), "adminUsername", "admin");
            var chain = context.getBean("springSecurityFilterChain", FilterChainProxy.class);
            for (String method : new String[]{"POST", "PUT", "PATCH", "DELETE"}) {
                assertAccess(chain, method, null, false);
                assertAccess(chain, method, "forged-token", false);
                assertAccess(chain, method, jwt.generateToken("other-user"), false);
                String token = jwt.generateToken("admin");
                assertAccess(chain, method, token.substring(0, token.lastIndexOf('.') + 1) + "AAAA", false);
                assertAccess(chain, method, token, true);
            }
        }
    }

    private void assertAccess(FilterChainProxy chain, String method, String token, boolean allowed) throws Exception {
        var request = new MockHttpServletRequest(method, "/api/admin/menu-items/1");
        request.setServletPath("/api/admin/menu-items/1");
        if (token != null) request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        var reachedController = new AtomicBoolean();
        chain.doFilter(request, response, (req, res) -> reachedController.set(true));
        assertEquals(allowed, reachedController.get(), method + " authorization");
        if (!allowed) assertTrue(response.getStatus() == 401 || response.getStatus() == 403);
    }
}
