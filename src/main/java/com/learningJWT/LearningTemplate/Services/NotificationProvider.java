package com.learningJWT.LearningTemplate.Services;

public interface NotificationProvider {
    void send(String to, String subject, String body);
    boolean isAvailable();
}
