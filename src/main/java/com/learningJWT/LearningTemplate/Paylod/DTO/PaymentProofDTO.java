package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.ProofStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProofDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long libraryId;
    private String screenshotUrl;
    private String description;
    private Double amountClaimed;
    private ProofStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    private String adminNote;
}
