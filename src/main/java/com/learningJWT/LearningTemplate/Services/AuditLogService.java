package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Enum.AuditAction;
import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(Library library, AuditAction action, String entityType,
                    String entityId, String description, String oldVal, String newVal) {
        User performer = getCurrentUser();
        String ip = getClientIp();
        String ua = getUserAgent();

        AuditLog log = AuditLog.builder()
                .performedBy(performer)
                .library(library)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .oldValue(oldVal)
                .newValue(newVal)
                .ipAddress(ip)
                .userAgent(ua)
                .performedAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    public void log(Library library, AuditAction action, String description) {
        log(library, action, null, null, description, null, null);
    }

    public List<AuditLog> getLibraryLogs(Long libraryId) {
        return auditLogRepository.findByLibraryIdOrderByPerformedAtDesc(libraryId);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop200ByOrderByPerformedAtDesc();
    }

    private User getCurrentUser() {
        try {
            Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (p instanceof UserDetails ud) {
                return userRepository.findByUsername(ud.getUsername()).orElse(null);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String xff = req.getHeader("X-Forwarded-For");
                return (xff != null) ? xff.split(",")[0].trim() : req.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs != null) return attrs.getRequest().getHeader("User-Agent");
        } catch (Exception ignored) {}
        return "unknown";
    }
}
