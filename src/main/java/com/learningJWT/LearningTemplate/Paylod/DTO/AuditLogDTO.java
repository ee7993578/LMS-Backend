package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.AuditAction;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String performedBy;
    private String libraryName;
    private AuditAction action;
    private String entityType;
    private String entityId;
    private String description;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private LocalDateTime performedAt;
}
