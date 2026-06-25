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

    @Column(length = 1000)
    private String description;

    /** Whether SuperAdmin has this plan available for assignment. Inactive plans stay
     *  visible to libraries already on them but cannot be newly assigned. */
    @Builder.Default
    private Boolean isActive = true;

    /** How many days a library can keep operating (with a warning banner) once
     *  studentCount exceeds noOfStudent but is still within noOfStudent + bufferStudent. */
    @Builder.Default
    private Integer gracePeriodDays = 3;

    @OneToMany(mappedBy = "libraryPlan")
    private List<Library> libraries;

}
