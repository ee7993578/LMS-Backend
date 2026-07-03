package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.StudentSubscriptionChangeType;
import com.learningJWT.LearningTemplate.Enum.StudentSubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * First-class subscription entity for a student. Every plan cycle (new / renewed / upgraded)
 * gets its own row, so the full billing + plan-change history is always available, and the
 * expiry date is a STORED, frozen value instead of being recomputed on the fly from
 * dateOfJoin + plan.subscriptionDays.
 */
@Entity
@Table(name = "student_subscription", indexes = {
        @Index(name = "idx_stsub_student", columnList = "student_id"),
        @Index(name = "idx_stsub_library", columnList = "library_id"),
        @Index(name = "idx_stsub_status", columnList = "status"),
        @Index(name = "idx_stsub_student_status", columnList = "student_id,status"),
        @Index(name = "idx_stsub_cycle_end", columnList = "cycle_end"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id")
    private Library library;

    @Column(name = "cycle_start", columnDefinition = "date", nullable = false)
    private LocalDate cycleStart;

    @Column(name = "cycle_end", columnDefinition = "date", nullable = false)
    private LocalDate cycleEnd;

    /** Snapshot of plan.subscriptionDays at the time this cycle was created (plan may change later). */
    private Integer planDurationDays;

    /** Snapshot of plan.price at the time this cycle was created (price history). */
    private Double planPrice;

    private Double payable;
    private Double paid;
    private Double balance;

    /** Unused value credited forward from the previous cycle when REPLACE mode is used. */
    @Builder.Default
    private Double carryForwardCredit = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private StudentSubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private StudentSubscriptionChangeType changeType;

    private Long previousSubscriptionId;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;

    private String createdBy;
}
