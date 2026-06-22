package com.learningJWT.LearningTemplate.Model;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "library_plan")
public class LibraryPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long planId;
    private String planName;
    private Double planPrice;
    private Integer noOfStudent;
    private Integer bufferStudent;
    private Integer planOrder;
    private Integer noOfDays;
    @OneToMany(mappedBy = "libraryPlan")
    private List<Library> libraries;

}
