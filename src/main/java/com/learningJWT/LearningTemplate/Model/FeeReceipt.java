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

    /** e.g. RCPT-2026-00124 */
    @Column(nullable = false, unique = true)
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
