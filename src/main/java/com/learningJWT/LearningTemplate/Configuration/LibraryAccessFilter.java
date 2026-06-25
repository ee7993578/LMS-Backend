package com.learningJWT.LearningTemplate.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Enum.UserRole;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * ===================================================================================
 * CENTRALIZED ACCESS CONTROL FILTER
 * ===================================================================================
 * This is the single place where every secured request is checked against:
 *   1. Library Exists      (LIBRARY_ADMIN / STUDENT requests must belong to a real library)
 *   2. Library Status      (TRIAL / TRIAL_READ_ONLY / ACTIVE / EXPIRED_READ_ONLY / INACTIVE / DELETED)
 *   3. Read-only enforcement (TRIAL_READ_ONLY and EXPIRED_READ_ONLY block all mutating requests)
 *
 * Plan-limit rules (point 4 of the brief) are enforced at the point of student creation
 * itself — see LibraryLifecycleService#canRegisterNewStudent, used from
 * LibraryAdminServiceImpl#createStudent — because that check needs request-body context
 * (how many students are being imported, etc.) that a generic filter can't see. Every other
 * cross-cutting rule (status + read-only) lives here so it is never duplicated in controllers.
 *
 * SuperAdmin requests are NEVER gated here — SuperAdmin must always be able to manage any
 * library regardless of that library's status.
 * ===================================================================================
 */
@Component
@RequiredArgsConstructor
public class LibraryAccessFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Paths that bypass this filter entirely (already public, or SuperAdmin-only). */
    private static final Set<String> BYPASS_PREFIXES = Set.of(
            "/api/auth", "/api/public", "/api/superadmin", "/uploads"
    );

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (BYPASS_PREFIXES.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // No authenticated principal yet — let Spring Security's own authorization rules
            // handle the 401/403; nothing for us to check here.
            filterChain.doFilter(request, response);
            return;
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // SuperAdmin is never subject to library-status gating.
        if (user.getRole() == UserRole.ROLE_SUPERADMIN) {
            filterChain.doFilter(request, response);
            return;
        }

        Library library = user.getLibrary();
        if (library == null) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                    "LIBRARY_NOT_FOUND", "No library is associated with this account.");
            return;
        }

        Status status = library.getStatus();
        if (status == null) {
            // Defensive: a library should never be persisted without a status, but if one
            // somehow is, fail closed rather than risk a NullPointerException on the switch.
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "LIBRARY_STATUS_UNKNOWN",
                    "Your library's status could not be determined. Please contact support.");
            return;
        }

        switch (status) {
            case DELETED:
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "LIBRARY_DELETED",
                        "This library no longer exists in the system.");
                return;

            case INACTIVE:
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "LIBRARY_INACTIVE",
                        user.getRole() == UserRole.ROLE_STUDENT
                                ? "Your library subscription is inactive. Please contact your library administrator."
                                : "Your library subscription is inactive. Please renew your subscription to continue.");
                return;

            case TRIAL_READ_ONLY:
                if (isMutating(request)) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "TRIAL_EXPIRED_READ_ONLY",
                            "Your trial has expired. Subscribe to continue using all features.");
                    return;
                }
                break;

            case EXPIRED_READ_ONLY:
                if (isMutating(request)) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "SUBSCRIPTION_EXPIRED_READ_ONLY",
                            "Your subscription has expired. Renew your subscription within 7 days.");
                    return;
                }
                break;

            case PENDING:
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "LIBRARY_PENDING",
                        "Your library has not been activated yet. Please contact support.");
                return;

            case TRIAL:
            case ACTIVE:
            default:
                // Full access — fall through.
                break;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isMutating(HttpServletRequest request) {
        return MUTATING_METHODS.contains(request.getMethod());
    }

    private void writeError(HttpServletResponse response, int statusCode, String code, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errorCode", code);
        body.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
