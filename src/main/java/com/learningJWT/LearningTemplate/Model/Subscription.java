package com.learningJWT.LearningTemplate.Model;
import com.learningJWT.LearningTemplate.Enum.Status;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long subscriptionId;

    @OneToOne
    @JoinColumn(name = "library_id", unique = true)
    private Library library;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private LibraryPlan libraryPlan;

    private String planName;
    private Integer studentLimit;
    private Integer bufferAllowed ;
    @Builder.Default
    private Integer bufferExpiryDays = 3;

    private Double pricePerMonth;
    private LocalDate billingStart;
    private LocalDate billingEnd;

    // See Library.status comment — VARCHAR avoids MySQL ENUM truncation when Status values change.
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(40)")
    private Status status;

    @Column(columnDefinition = "date")
    private LocalDate GraceEndDate;

    /** Timestamp of the last subscription status change (ACTIVE->EXPIRED, EXPIRED->INACTIVE, etc).
     *  Drives the "90 days of INACTIVE -> DELETED" scheduler rule. */
    @Column(columnDefinition = "datetime")
    private java.time.LocalDateTime statusChangedAt;

}
