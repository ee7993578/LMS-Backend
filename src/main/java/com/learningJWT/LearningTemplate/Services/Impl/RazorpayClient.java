package com.learningJWT.LearningTemplate.Services.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/**
 * Thin wrapper around the Razorpay REST API (Orders) using plain JDK HttpClient — no SDK
 * dependency needed for just "create order" + "verify signature".
 *
 * Docs: https://razorpay.com/docs/api/orders/ and
 *       https://razorpay.com/docs/payments/server-integration/php/payment-gateway/build-integration/#3-verify-payment-signature
 */
@Slf4j
@Service
public class RazorpayClient {

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    public String getKeyId() {
        return keyId;
    }

    /**
     * Creates a Razorpay order for the given amount (in paise) and returns the order id.
     * receipt is an arbitrary reference string we choose (we use it to tag what the order is for).
     */
    public String createOrder(long amountPaise, String currency, String receipt) throws Exception {
        if (!isConfigured()) {
            throw new Exception("Razorpay is not configured on this server. Set razorpay.key-id and razorpay.key-secret.");
        }

        Map<String, Object> body = Map.of(
                "amount", amountPaise,
                "currency", currency,
                "receipt", receipt,
                "payment_capture", 1
        );
        String json = objectMapper.writeValueAsString(body);

        String basicAuth = Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.razorpay.com/v1/orders"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + basicAuth)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Razorpay order creation failed: {} {}", response.statusCode(), response.body());
            throw new Exception("Could not create Razorpay order. Please try again.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = objectMapper.readValue(response.body(), Map.class);
        Object orderId = parsed.get("id");
        if (orderId == null) {
            throw new Exception("Razorpay did not return an order id.");
        }
        return orderId.toString();
    }

    /**
     * Verifies the signature Razorpay Checkout returns to the browser after a successful payment.
     * HMAC-SHA256 of "{order_id}|{payment_id}" using the key secret, hex-encoded, must equal signature.
     */
    public boolean verifyCheckoutSignature(String orderId, String paymentId, String signature) {
        if (signature == null || orderId == null || paymentId == null) return false;
        try {
            String payload = orderId + "|" + paymentId;
            String expected = hmacSha256Hex(payload, keySecret);
            return constantTimeEquals(expected, signature);
        } catch (Exception e) {
            log.error("Error verifying Razorpay checkout signature", e);
            return false;
        }
    }

    /**
     * Verifies an incoming webhook's X-Razorpay-Signature header against the raw request body,
     * using the separate webhook secret (configured in the Razorpay dashboard, not the key secret).
     */
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("razorpay.webhook-secret is not configured — rejecting webhook for safety.");
            return false;
        }
        if (rawBody == null || signatureHeader == null) return false;
        try {
            String expected = hmacSha256Hex(rawBody, webhookSecret);
            return constantTimeEquals(expected, signatureHeader);
        } catch (Exception e) {
            log.error("Error verifying Razorpay webhook signature", e);
            return false;
        }
    }

    private String hmacSha256Hex(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
