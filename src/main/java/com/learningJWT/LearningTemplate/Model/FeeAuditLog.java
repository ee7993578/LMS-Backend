package com.learningJWT.LearningTemplate.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_id")
    private Fee fee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    /** Who performed the action (admin username) */
    private String performedBy;

    /** e.g. VERIFY_PAYMENT, REJECT_PAYMENT, UPDATE_FEE, APPLY_CONCESSION, ADD_LATE_FEE */
    private String action;

    @Column(length = 2000)
    private String details;

    @Column(columnDefinition = "datetime")
    private LocalDateTime performedAt;
}
