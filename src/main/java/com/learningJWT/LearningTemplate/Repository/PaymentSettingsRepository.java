package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.PaymentSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentSettingsRepository extends JpaRepository<PaymentSettings, Long> {
    Optional<PaymentSettings> findByLibraryId(Long libraryId);
}
