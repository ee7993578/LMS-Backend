package com.learningJWT.LearningTemplate.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    // Per-IP buckets: 60 requests per minute for general APIs
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    // Stricter for auth endpoints: 10 attempts per 5 minutes
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    private Bucket getGeneralBucket(String ip) {
        return generalBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1))))
                        .build());
    }

    private Bucket getAuthBucket(String ip) {
        return authBuckets.computeIfAbsent(ip, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(5))))
                        .build());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req  = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String ip  = getClientIp(req);
        String uri = req.getRequestURI();

        boolean isAuth = uri.startsWith("/api/auth/");
        Bucket bucket  = isAuth ? getAuthBucket(ip) : getGeneralBucket(ip);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            res.setStatus(429);
            res.setContentType("application/json");
            String retryAfter = isAuth ? "300" : "60";
            res.setHeader("Retry-After", retryAfter);
            res.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\","
                    + "\"message\":\"Rate limit exceeded. Please try again later.\"}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
