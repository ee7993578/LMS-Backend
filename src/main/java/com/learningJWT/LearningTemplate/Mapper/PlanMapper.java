package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Enum.PlanDurationType;
import com.learningJWT.LearningTemplate.Model.Plan;
import com.learningJWT.LearningTemplate.Paylod.DTO.PlanDTO;

public class PlanMapper {

    public static PlanDTO toDto(Plan plan) {
        if (plan == null) return null;
        PlanDurationType type = plan.getDurationType() != null ? plan.getDurationType() : PlanDurationType.DAYS;
        return PlanDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .duration(plan.getDuration())
                .price(plan.getPrice())
                .libraryId(plan.getLibrary() != null ? plan.getLibrary().getId() : null)
                .hoursPerDay(plan.getHoursPerDay())
                .studyDays(plan.getStudyDays())
                .subscriptionDays(plan.getSubscriptionDays())
                .durationType(type.name())
                .durationMonths(plan.getDurationMonths())
                .build();
    }

    public static Plan toEntity(PlanDTO dto) {
        if (dto == null) return null;
        PlanDurationType type = PlanDurationType.DAYS;
        if (dto.getDurationType() != null) {
            try {
                type = PlanDurationType.valueOf(dto.getDurationType().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall back to DAYS on bad/unknown value
            }
        }
        return Plan.builder()
                .id(dto.getId())
                .name(dto.getName())
                .duration(dto.getDuration())
                .price(dto.getPrice())
                .hoursPerDay(dto.getHoursPerDay())
                .studyDays(dto.getStudyDays())
                .subscriptionDays(dto.getSubscriptionDays())
                .durationType(type)
                .durationMonths(dto.getDurationMonths())
                .build();
    }
}
