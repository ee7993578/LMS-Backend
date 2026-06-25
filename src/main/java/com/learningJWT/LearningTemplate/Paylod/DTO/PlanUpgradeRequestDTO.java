package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.PlanRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanUpgradeRequestDTO {
    private Long id;

    private Long libraryId;
    private String libraryName;

    private Long currentPlanId;
    private String currentPlanName;

    private Long requestedPlanId;
    private String requestedPlanName;
    private Double requestedPlanPrice;
    private Integer requestedPlanStudents;

    private String note;
    private PlanRequestStatus status;
    private String resolutionNote;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
