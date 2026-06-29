package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.AllocationMode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "seat_allocation",
        indexes = {
                @Index(name = "idx_sa_student_active", columnList = "student_id,active"),
                @Index(name = "idx_sa_slot_active", columnList = "slot_id,active"),
                @Index(name = "idx_sa_library_active", columnList = "library_id,active")
        }
)
public class SeatAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Changed from @OneToOne to @ManyToOne so a student can have multiple
    // non-overlapping allocations (different slots/time ranges)
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne
    @JoinColumn(name = "slot_id")
    private Slot slot;  // null for FLEXIBLE_HOUR mode

    @Enumerated(EnumType.STRING)
    private AllocationMode allocationMode;

    // For FLEXIBLE_HOUR mode
    private LocalTime flexStartTime;
    private LocalTime flexEndTime;

    private boolean active = true;

    @Column(columnDefinition = "datetime")
    private LocalDateTime allocatedAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime deallocatedAt;

    @Column(name = "end_date")
    private java.time.LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "library_id")
    private Library library;
}