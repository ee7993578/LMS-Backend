package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Model.PasswordResetToken;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Repository.PasswordResetTokenRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Qualifier("emailProvider")
    private final NotificationProvider emailProvider;

    @Transactional
    public void initiateReset(String email) {
        User user = userRepository.findByUsername(email).orElse(null);
        if (user == null) {
            // Don't reveal if email exists
            log.info("Password reset requested for unknown email: {}", email);
            return;
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = PasswordResetToken.builder()
                .token(token).user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false).build();
        tokenRepo.save(prt);

        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        emailProvider.send(email, "Password Reset Request",
                "Click the link below to reset your password (valid 30 minutes):\n\n"
                + resetLink + "\n\nIf you did not request this, please ignore this email.");
        log.info("Password reset token issued for user: {}", user.getUsername());
    }

    @Transactional
    public void resetPassword(String token, String newPassword) throws Exception {
        PasswordResetToken prt = tokenRepo.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new Exception("Invalid or expired reset token"));

        if (prt.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new Exception("Reset token has expired. Please request a new one.");

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsed(true);
        tokenRepo.save(prt);
        log.info("Password reset successful for user: {}", user.getUsername());
    }

    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepo.deleteExpiredTokens(LocalDateTime.now());
    }
}
