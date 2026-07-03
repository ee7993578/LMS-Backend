package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.RazorpayOrderResponseDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.RazorpayVerifyRequestDTO;

/**
 * Handles the "create library" flow when a paid plan is selected at signup time:
 *  - order-1 / free plan (or no plan)         -> create the library immediately, no payment.
 *  - any other (paid) plan                    -> stage the signup, return a Razorpay order;
 *                                                 the library is only actually created once
 *                                                 payment is verified.
 */
public interface LibrarySignupPaymentService {

    /** Step 1: decides whether payment is needed and either creates the library directly
     *  (free plan) or returns Razorpay order details (paid plan). */
    RazorpayOrderResponseDTO initiateSignup(LibraryDTO dto) throws Exception;

    /** Step 2 (paid plan only): verifies the Razorpay signature and, if valid, actually
     *  creates the library/admin/subscription using the staged signup data. */
    LibraryDTO verifySignup(RazorpayVerifyRequestDTO body) throws Exception;

    /** Same finalization as verifySignup, but called from the Razorpay webhook — which is
     *  already authenticated via the webhook secret, not the checkout signature — so the
     *  checkout signature is not re-checked here. Idempotent against verifySignup. */
    void finalizeSignupTrusted(String razorpayOrderId, String razorpayPaymentId) throws Exception;

    /** Called if the user closes/cancels the Razorpay checkout before paying, so the staged
     *  signup row doesn't linger forever as CREATED. */
    void cancelSignup(Long paymentRecordId) throws Exception;
}
