package com.learningJWT.LearningTemplate.Model;

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
    private Integer subscriptionDays; // kitne din ka subscription (same as duration usually)

    @ManyToOne
    @JoinColumn(name = "library_id")
    private Library library;
}
