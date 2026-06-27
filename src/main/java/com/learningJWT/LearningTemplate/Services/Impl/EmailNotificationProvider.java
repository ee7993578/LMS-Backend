package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Services.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service("emailProvider")
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationProvider implements NotificationProvider {

    private final JavaMailSender mailSender;

    @Override
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Email send failed to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
