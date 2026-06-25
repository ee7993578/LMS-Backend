package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.PlanRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A library admin's request to switch their library to a different plan. Created from the
 * Library Admin settings page ("Upgrade plan"), and resolved (approved/rejected) by SuperAdmin
 * from the SuperAdmin Plan Requests page. Approving actually changes the library's plan;
 * rejecting leaves everything untouched and just records the decision.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "plan_upgrade_request")
public class PlanUpgradeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "library_id")
    private Library library;

    @ManyToOne
    @JoinColumn(name = "current_plan_id")
    private LibraryPlan currentPlan;

    @ManyToOne
    @JoinColumn(name = "requested_plan_id")
    private LibraryPlan requestedPlan;

    @Column(length = 500)
    private String note; // optional message from the library admin explaining the request

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    @Builder.Default
    private PlanRequestStatus status = PlanRequestStatus.PENDING;

    @Column(length = 500)
    private String resolutionNote; // optional message from SuperAdmin (e.g. reason for rejection)

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime resolvedAt;
}
