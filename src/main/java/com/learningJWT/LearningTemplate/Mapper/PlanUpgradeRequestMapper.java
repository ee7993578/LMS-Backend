package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Model.PlanUpgradeRequest;
import com.learningJWT.LearningTemplate.Paylod.DTO.PlanUpgradeRequestDTO;

public class PlanUpgradeRequestMapper {

    public static PlanUpgradeRequestDTO toDTO(PlanUpgradeRequest req) {
        return PlanUpgradeRequestDTO.builder()
                .id(req.getId())
                .libraryId(req.getLibrary() != null ? req.getLibrary().getId() : null)
                .libraryName(req.getLibrary() != null ? req.getLibrary().getName() : null)
                .currentPlanId(req.getCurrentPlan() != null ? req.getCurrentPlan().getPlanId() : null)
                .currentPlanName(req.getCurrentPlan() != null ? req.getCurrentPlan().getPlanName() : null)
                .requestedPlanId(req.getRequestedPlan() != null ? req.getRequestedPlan().getPlanId() : null)
                .requestedPlanName(req.getRequestedPlan() != null ? req.getRequestedPlan().getPlanName() : null)
                .requestedPlanPrice(req.getRequestedPlan() != null ? req.getRequestedPlan().getPlanPrice() : null)
                .requestedPlanStudents(req.getRequestedPlan() != null ? req.getRequestedPlan().getNoOfStudent() : null)
                .note(req.getNote())
                .status(req.getStatus())
                .resolutionNote(req.getResolutionNote())
                .createdAt(req.getCreatedAt())
                .resolvedAt(req.getResolvedAt())
                .build();
    }
}
