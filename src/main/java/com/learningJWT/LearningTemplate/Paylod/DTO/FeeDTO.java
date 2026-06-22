package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.FeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeDTO {
    private Long feeId;
    private Long libraryId;
    private StudentDTO student;
    private Long studentId;
    private int monthId;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private double payable;
    private double Receive;
    private double balance;
    private double concession;
    private double lateFee;
    private FeeStatus feeStatus;
}
