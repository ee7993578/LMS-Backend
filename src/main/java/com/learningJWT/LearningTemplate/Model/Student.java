package com.learningJWT.LearningTemplate.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String phone;
    private String admissionNumber;

    @ManyToOne
    @JoinColumn(name = "library_id")
    private Library library;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @OneToMany(mappedBy = "student", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Attendance> attendanceList = new ArrayList<>();

    @OneToOne()
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Fee> fees = new ArrayList<>();

    // All allocations (active and history) - ManyToOne on SeatAllocation side
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeatAllocation> allocations = new ArrayList<>();

    // ── Self-Registration Fields ──────────────────────────────────────────────
    /** PENDING_APPROVAL | APPROVED | REJECTED */
    @Column(length = 30)
    private String registrationStatus; // null = admin-created (always approved)

    private String fatherName;
    private String address;
    private String photoUrl;
    private String aadharPhotoUrl;

    @Column(columnDefinition = "datetime")
    private java.time.LocalDateTime approvedAt;

    private String rejectionReason;

    private boolean active = true;
    private boolean buffer = false;

    @Column(columnDefinition = "date")
    private LocalDate dateOfJoin;

    @Column(columnDefinition = "datetime")
    private LocalDateTime bufferAssignedAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime bufferExpiry;
}
