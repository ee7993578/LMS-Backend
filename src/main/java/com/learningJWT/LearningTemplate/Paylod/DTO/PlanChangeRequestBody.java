package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for POST /api/libraryadmin/plan-requests */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanChangeRequestBody {
    private Long requestedPlanId;
    private String note;
}
