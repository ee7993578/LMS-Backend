package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.StudentSubscriptionChangeType;
import com.learningJWT.LearningTemplate.Enum.StudentSubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSubscriptionDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long planId;
    private String planName;
    private LocalDate cycleStart;
    private LocalDate cycleEnd;
    private Integer planDurationDays;
    private Double planPrice;
    private Double payable;
    private Double paid;
    private Double balance;
    private Double carryForwardCredit;
    private StudentSubscriptionStatus status;       // as stored
    private StudentSubscriptionStatus displayStatus; // computed (ACTIVE/EXPIRING_SOON/EXPIRED)
    private StudentSubscriptionChangeType changeType;
    private Long previousSubscriptionId;
    private Long daysRemaining;
    private LocalDateTime createdAt;
    private String createdBy;
    private Long feeId;
}
