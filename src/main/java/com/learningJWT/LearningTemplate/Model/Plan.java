package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.PlanDurationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private Long duration;       // subscription days (total validity)
    private double price;

    // NEW fields
    private Integer hoursPerDay;      // kitne hour per day study
    private Integer studyDays;        // kitne din study (active days in plan)
    private Integer subscriptionDays; // kitne din ka subscription (used when durationType = DAYS / custom)

    // Calendar-month based validity (1/2/3/4/6/12 months picked from dropdown).
    // When MONTHS, expiry = start date + durationMonths (same date next month/s).
    // Null or DAYS = old raw-day-count behaviour (custom option).
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlanDurationType durationType = PlanDurationType.DAYS;
    private Integer durationMonths; // used only when durationType = MONTHS

    @ManyToOne
    @JoinColumn(name = "library_id")
    private Library library;
}
