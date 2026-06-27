package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeAuditLogDTO {
    private Long id;
    private Long feeId;
    private Long studentId;
    private String studentName;
    private String performedBy;
    private String action;
    private String details;
    private LocalDateTime performedAt;
}
