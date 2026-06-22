package com.learningJWT.LearningTemplate.Model;

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
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String phone;

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

    private boolean active = true;
    private boolean buffer = false;

    @Column(columnDefinition = "datetime")
    private LocalDateTime bufferAssignedAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime bufferExpiry;
}
