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
    private Long duration;
    private double price;

    @ManyToOne
    @JoinColumn(name = "library_id")
    private Library library;
}
