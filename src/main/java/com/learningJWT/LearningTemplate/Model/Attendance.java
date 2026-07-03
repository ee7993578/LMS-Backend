package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Duration;
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
@Table(name = "attendance", indexes = {
    @Index(name = "idx_att_student_date",  columnList = "student_id,attendance_date"),
    @Index(name = "idx_att_library_date",  columnList = "library_id,attendance_date"),
    @Index(name = "idx_att_date",          columnList = "attendance_date"),
})
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id")
    private Library library;

    @Column(columnDefinition = "date")
    private LocalDate attendanceDate;

    @Column(columnDefinition = "datetime")
    private LocalDateTime shiftStart;

    @Column(columnDefinition = "datetime")
    private LocalDateTime shiftEnd;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    private Long totalStudyMinutes;
    private Long totalBreakMinutes;

    // Every punch-in/punch-out pair for this day is recorded as its own slot so the
    // student can see "Slot 1", "Slot 2" ... and the running total never resets when
    // they punch in again — it just keeps adding to totalStudyMinutes above.
    @OneToMany(mappedBy = "attendance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("slotNumber ASC")
    @Builder.Default
    private List<AttendanceSlot> slots = new ArrayList<>();
}
