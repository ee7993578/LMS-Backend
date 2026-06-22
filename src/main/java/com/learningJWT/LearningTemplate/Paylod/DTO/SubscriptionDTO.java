package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SubscriptionDTO {
    private Long subscriptionId;
    private Library library;
    private Long libraryId;
    private LibraryPlan libraryPlan;
    private Long libraryPlanId;
    private Status subscriptionStatus;
    private String planName;
    private Integer studentLimit;
    private Integer bufferAllowed ;
    private Integer bufferExpiryDays = 3;
    private Double pricePerMonth;
    private LocalDate billingStart;
    private LocalDate billingEnd;
    private LocalDate GraceEndDate;
}
