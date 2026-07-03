package com.learningJWT.LearningTemplate.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One punch-in -> punch-out session on a given day.
 * A student can have multiple slots on the same Attendance (same date):
 * Slot 1: 10:00 - 10:45, Slot 2: 11:15 - 13:00, etc.
 * Attendance.totalStudyMinutes is always the sum of every slot's durationMinutes,
 * so re-punching in never resets the day's total — it just opens the next slot.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "attendance_slot", indexes = {
        @Index(name = "idx_slot_attendance", columnList = "attendance_id"),
})
public class AttendanceSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    // 1, 2, 3 ... in order within the day
    private Integer slotNumber;

    @Column(columnDefinition = "datetime")
    private LocalDateTime punchIn;

    // null while the slot is still open (student is currently punched in)
    @Column(columnDefinition = "datetime")
    private LocalDateTime punchOut;

    // filled in once the slot is closed (punch out, or the midnight auto-close)
    private Long durationMinutes;

    // true only for slots the midnight scheduler had to auto-close because the
    // student forgot to punch out
    @Builder.Default
    private Boolean autoClosed = false;
}
