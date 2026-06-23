package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Model.PaymentSettings;
import com.learningJWT.LearningTemplate.Paylod.DTO.PaymentSettingsDTO;

public class PaymentSettingsMapper {

    public static PaymentSettingsDTO toDTO(PaymentSettings entity, String baseUrl) {
        if (entity == null) return null;
        return PaymentSettingsDTO.builder()
                .id(entity.getId())
                .libraryId(entity.getLibrary() != null ? entity.getLibrary().getId() : null)
                .qrImageUrl(entity.getQrImagePath() != null ? baseUrl + "/uploads/" + entity.getQrImagePath() : null)
                .upiId(entity.getUpiId())
                .phoneNumber(entity.getPhoneNumber())
                .description(entity.getDescription())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
