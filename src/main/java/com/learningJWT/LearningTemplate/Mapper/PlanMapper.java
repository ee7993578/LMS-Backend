package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Model.Plan;
import com.learningJWT.LearningTemplate.Paylod.DTO.PlanDTO;

public class PlanMapper {

    public static PlanDTO toDto(Plan plan) {
        if (plan == null) return null;
        return PlanDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .duration(plan.getDuration())
                .price(plan.getPrice())
                .libraryId(plan.getLibrary() != null ? plan.getLibrary().getId() : null)
                .hoursPerDay(plan.getHoursPerDay())
                .studyDays(plan.getStudyDays())
                .subscriptionDays(plan.getSubscriptionDays())
                .build();
    }

    public static Plan toEntity(PlanDTO dto) {
        if (dto == null) return null;
        return Plan.builder()
                .id(dto.getId())
                .name(dto.getName())
                .duration(dto.getDuration())
                .price(dto.getPrice())
                .hoursPerDay(dto.getHoursPerDay())
                .studyDays(dto.getStudyDays())
                .subscriptionDays(dto.getSubscriptionDays())
                .build();
    }
}
