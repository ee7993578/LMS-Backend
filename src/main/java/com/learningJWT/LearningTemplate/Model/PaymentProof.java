package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.ProofStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProof {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    /** Relative path under the uploads dir, e.g. "proof/5_1719999999.jpg". Optional. */
    private String screenshotPath;

    /** Optional free-text note from the student about the payment. */
    @Column(length = 1000)
    private String description;

    /** Amount the student claims to have paid (optional, admin can adjust on verify). */
    private Double amountClaimed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProofStatus status;

    @Column(columnDefinition = "datetime")
    private LocalDateTime submittedAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime verifiedAt;

    /** Admin note shown back to the student, e.g. reason for rejection. */
    @Column(length = 1000)
    private String adminNote;
}
