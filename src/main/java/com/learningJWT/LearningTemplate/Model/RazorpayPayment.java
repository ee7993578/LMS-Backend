package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.RazorpayPaymentStatus;
import com.learningJWT.LearningTemplate.Enum.RazorpayPurpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks every Razorpay order we create, regardless of what it's for (paid library signup,
 * or a library admin's paid plan upgrade). One row per checkout attempt.
 *
 * Flow:
 *  1. We create a Razorpay order -> save a row here with status=CREATED, razorpayOrderId set.
 *  2. Frontend opens Razorpay Checkout with that order id.
 *  3. On success, frontend calls our /verify endpoint with the payment id + signature.
 *     We verify the signature, mark status=PAID, and apply the actual side-effect
 *     (finalize library signup, or apply the plan upgrade) in the SAME transaction.
 *  4. Razorpay also calls our webhook independently (covers the case where the user's
 *     browser dies after paying but before /verify runs) — idempotent against step 3.
 *  5. If the user closes the checkout without paying, frontend calls /cancel and we
 *     mark status=CANCELLED so the row doesn't linger as a phantom pending request.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "razorpay_payment")
public class RazorpayPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(40)")
    private RazorpayPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private RazorpayPaymentStatus status = RazorpayPaymentStatus.CREATED;

    @Column(unique = true)
    private String razorpayOrderId;

    private String razorpayPaymentId;

    @Column(length = 512)
    private String razorpaySignature;

    /** Amount in paise (smallest currency unit), exactly as sent to Razorpay. */
    private Long amountPaise;

    @Builder.Default
    private String currency = "INR";

    /** For LIBRARY_SIGNUP: the LibraryPlan being purchased. For LIBRARY_PLAN_UPGRADE: the
     *  requested LibraryPlan. Kept as a raw id (not a relation) since for LIBRARY_SIGNUP the
     *  library doesn't exist yet at order-creation time. */
    private Long planId;

    /** Set once the library actually exists — null for LIBRARY_SIGNUP until /verify succeeds. */
    private Long libraryId;

    /** Set only for LIBRARY_PLAN_UPGRADE — links to the PlanUpgradeRequest audit row. */
    private Long planUpgradeRequestId;

    /** For LIBRARY_SIGNUP only: the full signup form (name/email/admin credentials/etc),
     *  serialized as JSON. We stage it here instead of creating the Library/User rows up
     *  front, so an abandoned/failed payment never leaves a half-created library or admin
     *  account behind. Cleared (left as-is, harmless) once consumed by /verify. */
    @Column(columnDefinition = "TEXT")
    private String stagedPayloadJson;

    /** Free-text note, e.g. failure reason from Razorpay or our own validation error. */
    @Column(length = 500)
    private String note;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedAt;
}
