package com.learningJWT.LearningTemplate.Enum;

/** Lifecycle status of a RazorpayPayment row. */
public enum RazorpayPaymentStatus {
    /** Order created at Razorpay, checkout shown to user, no result yet. */
    CREATED,
    /** Signature verified, payment captured, our side-effect (signup/upgrade) applied. */
    PAID,
    /** Razorpay reported a failed payment, or our signature verification failed. */
    FAILED,
    /** User closed the checkout / explicitly cancelled before paying. */
    CANCELLED
}
