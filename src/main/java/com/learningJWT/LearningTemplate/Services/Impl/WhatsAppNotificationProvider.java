package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Services.NotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WhatsApp provider stub — swap with Twilio/MSG91/Meta API integration.
 * Configure via: notification.whatsapp.api-url, notification.whatsapp.api-key
 */
@Service("whatsAppProvider")
@Slf4j
public class WhatsAppNotificationProvider implements NotificationProvider {

    @Override
    public void send(String to, String subject, String body) {
        // TODO: Integrate with Twilio/MSG91/Meta Cloud API
        // Example URL: https://wa.me/api/send?phone={to}&message={body}
        log.info("[WhatsApp STUB] To: {} | Msg: {}", to, body);
    }

    @Override
    public boolean isAvailable() {
        return false; // Set true when real provider configured
    }
}
