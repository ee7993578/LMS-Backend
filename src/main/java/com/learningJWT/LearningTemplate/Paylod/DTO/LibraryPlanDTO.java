package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryPlanDTO {

    private Long planId;
    private String planName;
    private Double planPrice;
    private Integer noOfStudent;
    private Integer bufferStudent;
    private Integer planOrder;
    private Integer noOfDays;
}
