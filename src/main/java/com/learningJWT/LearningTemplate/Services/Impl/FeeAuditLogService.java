package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeAuditLogDTO;
import com.learningJWT.LearningTemplate.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeAuditLogService {

    private final FeeAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) return ud.getUsername();
        return "system";
    }

    public void log(Fee fee, Library library, Student student, String action, String details) {
        FeeAuditLog log = FeeAuditLog.builder()
                .fee(fee)
                .library(library)
                .student(student)
                .performedBy(getCurrentUsername())
                .action(action)
                .details(details)
                .performedAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    public List<FeeAuditLogDTO> getLibraryAuditLogs(Long libraryId) {
        return auditLogRepository.findByLibraryIdOrderByPerformedAtDesc(libraryId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<FeeAuditLogDTO> getStudentAuditLogs(Long studentId) {
        return auditLogRepository.findByStudentIdOrderByPerformedAtDesc(studentId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private FeeAuditLogDTO toDTO(FeeAuditLog a) {
        return FeeAuditLogDTO.builder()
                .id(a.getId())
                .feeId(a.getFee() != null ? a.getFee().getFeeId() : null)
                .studentId(a.getStudent() != null ? a.getStudent().getId() : null)
                .studentName(a.getStudent() != null ? a.getStudent().getFullName() : null)
                .performedBy(a.getPerformedBy())
                .action(a.getAction())
                .details(a.getDetails())
                .performedAt(a.getPerformedAt())
                .build();
    }
}
