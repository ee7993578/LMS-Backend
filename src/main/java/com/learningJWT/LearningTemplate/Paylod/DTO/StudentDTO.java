package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String admissionNumber;
    private SeatDTO seat;
    private PlanDTO plan;
    private Long planId;
    private Long seatId;
    private String username;
    private String password;
    private LocalDate dateOfJoin;
    private LocalDate subscriptionExpiryDate; // computed: dateOfJoin + plan.subscriptionDays
}
