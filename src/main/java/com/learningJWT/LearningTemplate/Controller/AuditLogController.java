package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Model.AuditLog;
import com.learningJWT.LearningTemplate.Paylod.DTO.AuditLogDTO;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @GetMapping("/api/libraryadmin/audit-log")
    public ResponseEntity<?> libraryLog() {
        try {
            Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = (p instanceof UserDetails ud) ? ud.getUsername() : null;
            var user = userRepository.findByUsername(username).orElseThrow();
            Long libId = user.getLibrary().getId();
            return ResponseEntity.ok(auditLogService.getLibraryLogs(libId)
                    .stream().map(this::toDTO).collect(Collectors.toList()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping("/api/superadmin/audit-log")
    public ResponseEntity<?> allLog() {
        return ResponseEntity.ok(auditLogService.getRecentLogs()
                .stream().map(this::toDTO).collect(Collectors.toList()));
    }

    private AuditLogDTO toDTO(AuditLog a) {
        return AuditLogDTO.builder()
                .id(a.getId())
                .performedBy(a.getPerformedBy() != null ? a.getPerformedBy().getUsername() : "system")
                .libraryName(a.getLibrary() != null ? a.getLibrary().getName() : null)
                .action(a.getAction()).entityType(a.getEntityType()).entityId(a.getEntityId())
                .description(a.getDescription()).oldValue(a.getOldValue()).newValue(a.getNewValue())
                .ipAddress(a.getIpAddress()).performedAt(a.getPerformedAt())
                .build();
    }
}
