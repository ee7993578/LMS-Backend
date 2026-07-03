package com.learningJWT.LearningTemplate.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_receipt", indexes = {
    @Index(name = "idx_receipt_student",  columnList = "student_id"),
    @Index(name = "idx_receipt_library",  columnList = "library_id"),
    @Index(name = "idx_receipt_fee",      columnList = "fee_id"),
    @Index(name = "idx_receipt_date",     columnList = "payment_date"),
}, uniqueConstraints = {
    // Receipt numbers only need to be unique WITHIN a library, not globally.
    // (Library code is embedded in the number itself, so cross-library collisions
    // can't happen in practice, but this constraint is the real safety net.)
    @UniqueConstraint(name = "uk_receipt_library_number", columnNames = {"library_id", "receipt_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. RCPT-LIB-A3K9-2026-00124 (library code embedded so numbers never clash across libraries) */
    @Column(nullable = false)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_id", nullable = false)
    private Fee fee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    /** Amount received in this payment event */
    private double amountPaid;

    private double concession;
    private double lateFee;
    private double balanceAfter;

    private String paymentMode;
    private String transactionRef;

    @Column(columnDefinition = "date")
    private LocalDate paymentDate;

    @Column(columnDefinition = "datetime")
    private LocalDateTime generatedAt;

    /** Which PaymentProof triggered this receipt (nullable for manual entries) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proof_id")
    private PaymentProof proof;
}
