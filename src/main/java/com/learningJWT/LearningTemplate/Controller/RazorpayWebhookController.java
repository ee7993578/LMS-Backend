package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Services.Impl.RazorpayWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Razorpay webhook receiver. Configure this URL (e.g. https://yourdomain.com/api/public/webhooks/razorpay)
 * in the Razorpay Dashboard -> Settings -> Webhooks, subscribed to at least:
 *   payment.captured, payment.failed, order.paid
 * and set the same secret there as razorpay.webhook-secret in application.properties.
 *
 * Lives under /api/public/** (already permitAll in SecurityConfig) since Razorpay can't send
 * our JWT — authenticity instead comes entirely from the HMAC signature check inside the service.
 */
@Slf4j
@RestController
@RequestMapping("/api/public/webhooks")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final RazorpayWebhookService webhookService;

    @PostMapping(value = "/razorpay", consumes = "application/json")
    public ResponseEntity<?> receiveWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String rawBody) {
        try {
            webhookService.handleWebhook(rawBody, signature);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Razorpay webhook rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}