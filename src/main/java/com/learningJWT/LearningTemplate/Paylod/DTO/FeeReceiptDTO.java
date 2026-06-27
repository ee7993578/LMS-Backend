package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeReceiptDTO {

    private Long id;
    private String receiptNumber;

    // Library details
    private String libraryName;
    private String libraryAddress;
    private String libraryPhone;
    private String libraryGst;

    // Student details
    private String studentName;
    private String admissionNumber;
    private String studentPhone;
    private String seatNumber;
    private String membershipPlan;
    private Long studentId;

    // Payment details
    private LocalDate paymentDate;
    private String paymentMode;
    private String transactionRef;

    // Fee breakdown
    private double monthlyFee;
    private double lateFee;
    private double concession;
    private double amountPaid;
    private double balanceAfter;
    private int monthId;
    private String monthLabel;

    // Full fee record snapshot for ledger
    private FeeDTO fee;

    private LocalDateTime generatedAt;
    private Long proofId;
}
