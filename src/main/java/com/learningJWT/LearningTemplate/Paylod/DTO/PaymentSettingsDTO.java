package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSettingsDTO {
    private Long id;
    private Long libraryId;
    private String qrImageUrl;
    private String upiId;
    private String phoneNumber;
    private String description;
    private LocalDateTime updatedAt;
}
