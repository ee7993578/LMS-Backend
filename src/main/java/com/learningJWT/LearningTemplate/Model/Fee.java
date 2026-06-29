package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.FeeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
@Entity
@Table(name = "fee", indexes = {
    @Index(name = "idx_fee_library",          columnList = "library_id"),
    @Index(name = "idx_fee_student",          columnList = "student_id"),
    @Index(name = "idx_fee_lib_month",        columnList = "library_id,month_id"),
    @Index(name = "idx_fee_lib_status",       columnList = "library_id,fee_status"),
    @Index(name = "idx_fee_lib_month_status", columnList = "library_id,month_id,fee_status"),
})
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
