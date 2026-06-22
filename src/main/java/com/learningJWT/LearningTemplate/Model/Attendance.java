package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
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
}
