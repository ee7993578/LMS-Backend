package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by any "/initiate" payment endpoint. Frontend uses this to open Razorpay Checkout.
 * If requiresPayment is false, the action (library created / plan upgraded) already happened
 * and "result" holds the normal response — no checkout needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponseDTO {
    private boolean requiresPayment;

    // Present when requiresPayment = true
    private String razorpayOrderId;
    private String keyId;
    private Long amountPaise;
    private String currency;
    private Long paymentRecordId;
    private String planName;

    // Present when requiresPayment = false (free/order-1 plan — action applied immediately)
    private Object result;
}
