package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body sent by the frontend after Razorpay Checkout succeeds, to verify + finalize. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayVerifyRequestDTO {
    private Long paymentRecordId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}
