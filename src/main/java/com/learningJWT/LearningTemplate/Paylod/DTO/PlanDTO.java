package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDTO {
    private Long id;
    private String name;
    private Long duration;         // subscription days
    private double price;
    private Long libraryId;
    // NEW
    private Integer hoursPerDay;
    private Integer studyDays;
    private Integer subscriptionDays;

    // "MONTHS" (dropdown: 1/2/3/4/6/12) or "DAYS" (custom option). Defaults to DAYS.
    private String durationType;
    private Integer durationMonths; // used only when durationType = MONTHS
}
