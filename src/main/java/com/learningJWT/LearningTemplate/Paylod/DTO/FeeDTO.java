package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    // Jackson's default bean-name mangling lowercases "Receive" -> "receive" for
    // getReceive()/setReceive(), which silently breaks JSON binding for any request
    // body sent with the capitalized key "Receive" (as the frontend does throughout
    // the app). Pinning the exact property name here fixes that on both read and write.
    @JsonProperty("Receive")
    private double Receive;

    private double balance;
    private double concession;
    private double lateFee;
    private FeeStatus feeStatus;
    private String paymentMode;
    private String transactionRef;
}
