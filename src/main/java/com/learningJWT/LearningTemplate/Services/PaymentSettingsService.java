package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.PaymentSettingsDTO;
import org.springframework.web.multipart.MultipartFile;

public interface PaymentSettingsService {

    /** Get the logged-in admin's library payment settings. Returns null fields if not yet configured. */
    PaymentSettingsDTO getMySettings() throws Exception;

    /** Get payment settings for the logged-in student's library (read-only, deposit page). */
    PaymentSettingsDTO getSettingsForStudent() throws Exception;

    /**
     * Create/update payment settings for the logged-in admin's library.
     * qrFile is optional — if null, the existing QR image (if any) is kept.
     */
    PaymentSettingsDTO saveSettings(MultipartFile qrFile, String upiId, String phoneNumber, String description) throws Exception;
}
