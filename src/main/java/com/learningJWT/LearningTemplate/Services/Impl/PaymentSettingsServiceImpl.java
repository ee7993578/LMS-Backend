package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Mapper.PaymentSettingsMapper;
import com.learningJWT.LearningTemplate.Model.PaymentSettings;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.PaymentSettingsDTO;
import com.learningJWT.LearningTemplate.Repository.PaymentSettingsRepository;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.PaymentSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentSettingsServiceImpl implements PaymentSettingsService {

    private final PaymentSettingsRepository paymentSettingsRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private User getLoggedInAdmin() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new Exception("Logged-in user not found"));
        }
        throw new Exception("No authenticated user found");
    }

    @Override
    public PaymentSettingsDTO getMySettings() throws Exception {
        User admin = getLoggedInAdmin();
        Long libraryId = admin.getLibrary().getId();
        PaymentSettings settings = paymentSettingsRepository.findByLibraryId(libraryId).orElse(null);
        if (settings == null) {
            // Not configured yet — return an empty shell so the frontend can render the form.
            return PaymentSettingsDTO.builder().libraryId(libraryId).build();
        }
        return PaymentSettingsMapper.toDTO(settings, baseUrl);
    }

    @Override
    public PaymentSettingsDTO getSettingsForStudent() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new Exception("No authenticated user found");
        }
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new Exception("Logged-in user not found"));
        Student student = studentRepository.findByUserId(user.getId());
        if (student == null || student.getLibrary() == null) {
            throw new Exception("Student or library not found");
        }
        PaymentSettings settings = paymentSettingsRepository.findByLibraryId(student.getLibrary().getId()).orElse(null);
        if (settings == null) {
            return PaymentSettingsDTO.builder().libraryId(student.getLibrary().getId()).build();
        }
        return PaymentSettingsMapper.toDTO(settings, baseUrl);
    }

    @Override
    public PaymentSettingsDTO saveSettings(MultipartFile qrFile, String upiId, String phoneNumber, String description) throws Exception {
        User admin = getLoggedInAdmin();
        Long libraryId = admin.getLibrary().getId();

        PaymentSettings settings = paymentSettingsRepository.findByLibraryId(libraryId).orElse(null);
        if (settings == null) {
            settings = PaymentSettings.builder()
                    .library(admin.getLibrary())
                    .build();
        }

        if (qrFile != null && !qrFile.isEmpty()) {
            String oldPath = settings.getQrImagePath();
            String newPath = fileStorageService.store(qrFile, "qr");
            settings.setQrImagePath(newPath);
            if (oldPath != null) fileStorageService.delete(oldPath);
        }

        settings.setUpiId(upiId);
        settings.setPhoneNumber(phoneNumber);
        settings.setDescription(description);
        settings.setUpdatedAt(LocalDateTime.now());

        PaymentSettings saved = paymentSettingsRepository.save(settings);
        return PaymentSettingsMapper.toDTO(saved, baseUrl);
    }
}
