package com.learningJWT.LearningTemplate.Services.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningJWT.LearningTemplate.Enum.RazorpayPaymentStatus;
import com.learningJWT.LearningTemplate.Model.RazorpayPayment;
import com.learningJWT.LearningTemplate.Repository.RazorpayPaymentRepository;
import com.learningJWT.LearningTemplate.Services.LibrarySignupPaymentService;
import com.learningJWT.LearningTemplate.Services.PlanUpgradeRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handles incoming Razorpay webhook events — the safety net that finalizes a payment even if
 * the user's browser closes/crashes right after paying but before our synchronous /verify call
 * from the frontend completes. Razorpay calls this independently of the browser.
 *
 * Both code paths (synchronous /verify and this webhook) converge on the same apply logic via
 * the *Trusted finalize methods on the two payment services, and both are idempotent — whichever
 * arrives first does the work, the second is a no-op.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final RazorpayPaymentRepository razorpayPaymentRepository;
    private final RazorpayClient razorpayClient;
    private final LibrarySignupPaymentService librarySignupPaymentService;
    private final PlanUpgradeRequestService planUpgradeRequestService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param rawBody         the exact raw request body Razorpay sent — signature is computed
     *                        over this exact string, so it must not be re-serialized first.
     * @param signatureHeader the X-Razorpay-Signature header value
     */
    @Transactional
    public void handleWebhook(String rawBody, String signatureHeader) throws Exception {
        if (!razorpayClient.verifyWebhookSignature(rawBody, signatureHeader)) {
            log.warn("Rejected Razorpay webhook with invalid signature");
            throw new Exception("Invalid webhook signature");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> event = objectMapper.readValue(rawBody, Map.class);
        String eventType = String.valueOf(event.get("event"));

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) event.get("payload");
        if (payload == null) return;

        switch (eventType) {
            case "payment.captured", "order.paid" -> handlePaymentCaptured(payload);
            case "payment.failed" -> handlePaymentFailed(payload);
            default -> log.info("Ignoring unhandled Razorpay webhook event: {}", eventType);
        }
    }

    @SuppressWarnings("unchecked")
    private void handlePaymentCaptured(Map<String, Object> payload) throws Exception {
        Map<String, Object> paymentEntity = extractEntity(payload, "payment");
        if (paymentEntity == null) return;

        String orderId = String.valueOf(paymentEntity.get("order_id"));
        String paymentId = String.valueOf(paymentEntity.get("id"));

        RazorpayPayment payment = razorpayPaymentRepository.findByRazorpayOrderId(orderId).orElse(null);
        if (payment == null) {
            log.warn("Webhook payment.captured for unknown order_id={}", orderId);
            return;
        }
        if (payment.getStatus() == RazorpayPaymentStatus.PAID) {
            return; // already finalized by the synchronous /verify call
        }

        switch (payment.getPurpose()) {
            case LIBRARY_SIGNUP -> librarySignupPaymentService.finalizeSignupTrusted(orderId, paymentId);
            case LIBRARY_PLAN_UPGRADE -> planUpgradeRequestService.finalizeUpgradeTrusted(orderId, paymentId);
        }
    }

    private void handlePaymentFailed(Map<String, Object> payload) {
        Map<String, Object> paymentEntity = extractEntity(payload, "payment");
        if (paymentEntity == null) return;
        String orderId = String.valueOf(paymentEntity.get("order_id"));

        razorpayPaymentRepository.findByRazorpayOrderId(orderId).ifPresent(payment -> {
            if (payment.getStatus() == RazorpayPaymentStatus.CREATED) {
                payment.setStatus(RazorpayPaymentStatus.FAILED);
                payment.setNote("Razorpay reported payment.failed");
                payment.setUpdatedAt(LocalDateTime.now());
                razorpayPaymentRepository.save(payment);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractEntity(Map<String, Object> payload, String key) {
        Object node = payload.get(key);
        if (!(node instanceof Map)) return null;
        Object entity = ((Map<String, Object>) node).get("entity");
        return entity instanceof Map ? (Map<String, Object>) entity : null;
    }
}
