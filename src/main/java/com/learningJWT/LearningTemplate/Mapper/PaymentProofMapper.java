package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Model.PaymentProof;
import com.learningJWT.LearningTemplate.Paylod.DTO.PaymentProofDTO;

public class PaymentProofMapper {

    public static PaymentProofDTO toDTO(PaymentProof entity, String baseUrl) {
        if (entity == null) return null;
        return PaymentProofDTO.builder()
                .id(entity.getId())
                .studentId(entity.getStudent() != null ? entity.getStudent().getId() : null)
                .studentName(entity.getStudent() != null ? entity.getStudent().getFullName() : null)
                .libraryId(entity.getLibrary() != null ? entity.getLibrary().getId() : null)
                .screenshotUrl(entity.getScreenshotPath() != null ? baseUrl + "/uploads/" + entity.getScreenshotPath() : null)
                .description(entity.getDescription())
                .amountClaimed(entity.getAmountClaimed())
                .status(entity.getStatus())
                .submittedAt(entity.getSubmittedAt())
                .verifiedAt(entity.getVerifiedAt())
                .adminNote(entity.getAdminNote())
                .build();
    }
}
