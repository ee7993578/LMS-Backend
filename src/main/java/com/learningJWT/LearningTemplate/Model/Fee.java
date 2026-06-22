package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.FeeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fee {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feeId;
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private int monthId;
    @Column(columnDefinition = "date", nullable = false)
    private LocalDate dueDate;
    @Column(columnDefinition = "date")
    private LocalDate paymentDate;
    @Column(nullable = false)
    private double payable;
    private double Receive;
    private double balance;
    private double concession;
    private double lateFee;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeStatus feeStatus;

}
