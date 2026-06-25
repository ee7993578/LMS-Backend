package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.AllocationMode;
import com.learningJWT.LearningTemplate.Enum.AttendanceMode;
import com.learningJWT.LearningTemplate.Enum.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Library {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;
    private String email;
    private String phone;
    private String website;

    // IMPORTANT: explicit VARCHAR columnDefinition. Without this, Hibernate's ddl-auto=update
    // creates a MySQL ENUM(...) column baked with whatever Status values existed at the time
    // the table was first created. Adding new enum values later (TRIAL, TRIAL_READ_ONLY, etc.)
    // then fails at runtime with "Data truncated for column 'status'" because ddl-auto=update
    // never widens an existing ENUM's allowed values. VARCHAR has no such constraint.
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(40)")
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_mode")
    private AllocationMode allocationMode = AllocationMode.FLEXIBLE_HOUR;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_mode")
    private AttendanceMode attendanceMode = AttendanceMode.BOTH;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    // ===== Subscription / Trial lifecycle tracking =====

    /** When the 7-day TRIAL period started (set at registration). */
    @Column(columnDefinition = "datetime")
    private LocalDateTime trialStartDate;

    /** trialStartDate + 7 days. After this, library moves to TRIAL_READ_ONLY. */
    @Column(columnDefinition = "datetime")
    private LocalDateTime trialEndDate;

    /** Timestamp of the most recent status transition. Used to compute how long a
     *  library has been in TRIAL_READ_ONLY / EXPIRED_READ_ONLY / INACTIVE, which then
     *  drives the next automatic transition (e.g. INACTIVE -> DELETED after 30/90 days). */
    @Column(columnDefinition = "datetime")
    private LocalDateTime statusChangedAt;

    /** Plan-usage grace period (student count between plan limit and limit+buffer).
     *  Set when the library first exceeds its plan's student limit; cleared when the
     *  library is back within limit or the plan/limit changes. */
    @Column(columnDefinition = "datetime")
    private LocalDateTime gracePeriodStartedAt;

    @OneToOne
    @JoinColumn(name = "admin_id", unique = true)
    private User admin;

    @OneToMany(mappedBy = "library")
    private List<Student> students;

    @OneToMany(mappedBy = "library", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Fee> fees = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private LibraryPlan libraryPlan;

    @OneToMany(mappedBy = "library", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Slot> slots = new ArrayList<>();
}
