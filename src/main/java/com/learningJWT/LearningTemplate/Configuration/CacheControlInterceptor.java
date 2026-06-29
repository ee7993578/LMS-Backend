package com.learningJWT.LearningTemplate.Configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CacheControlInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) {
        String uri = request.getRequestURI();

        // Public/static data — cache 5 minutes
        if (uri.startsWith("/api/public/")) {
            response.setHeader("Cache-Control", "public, max-age=300");
        }
        // Authenticated data — no cache (always fresh)
        else if (uri.startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
        }
        // Uploaded files — cache 7 days
        else if (uri.startsWith("/uploads/")) {
            response.setHeader("Cache-Control", "public, max-age=604800, immutable");
        }

        return true;
    }
}
