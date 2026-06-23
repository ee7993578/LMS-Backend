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
}
