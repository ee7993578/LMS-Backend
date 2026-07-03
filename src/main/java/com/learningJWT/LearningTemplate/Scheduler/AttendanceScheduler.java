package com.learningJWT.LearningTemplate.Scheduler;

import com.learningJWT.LearningTemplate.Enum.AttendanceStatus;
import com.learningJWT.LearningTemplate.Model.Attendance;
import com.learningJWT.LearningTemplate.Model.AttendanceSlot;
import com.learningJWT.LearningTemplate.Repository.AttendanceRepository;
import com.learningJWT.LearningTemplate.Repository.AttendanceSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Midnight sweep, two jobs in one:
 *
 *  1. Auto punch-out: if a student forgot to punch out, their open slot from "yesterday"
 *     is closed at exactly 23:59:59 of that date and its minutes are banked into
 *     Attendance.totalStudyMinutes, so the day's total is still correct.
 *
 *  2. Slot cleanup: once a day is finished, its Slot 1 / Slot 2 / ... detail rows are no
 *     longer needed (they were only there so today's UI could show live sessions) -- so
 *     they're deleted. Attendance.totalStudyMinutes itself is NEVER touched by this step,
 *     so weekly/monthly progress keeps working exactly as before; only the slot-level
 *     breakdown for past days disappears.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AttendanceScheduler {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceSlotRepository attendanceSlotRepository;

    @Scheduled(cron = "0 0 0 * * *") // every day at 00:00:00
    @Transactional
    public void autoPunchOutForgottenSessions() {
        log.info("Running midnight auto punch-out sweep");

        List<AttendanceSlot> openSlots = attendanceSlotRepository.findByPunchOutIsNull();
        int closedCount = 0;

        for (AttendanceSlot slot : openSlots) {
            Attendance attendance = slot.getAttendance();
            if (attendance == null || attendance.getStatus() != AttendanceStatus.IN) continue;

            LocalDate attendanceDate = attendance.getAttendanceDate();
            // Only close out slots left open from a PAST day -- if today's date is still
            // being lived through (edge case: scheduler firing exactly as a student
            // punches in), leave it alone.
            if (attendanceDate == null || !attendanceDate.isBefore(LocalDate.now())) continue;

            LocalDateTime dayEnd = LocalDateTime.of(attendanceDate, LocalTime.of(23, 59, 59));
            LocalDateTime punchIn = slot.getPunchIn() != null ? slot.getPunchIn() : dayEnd;

            long slotMinutes = Math.max(0, Duration.between(punchIn, dayEnd).toMinutes());

            slot.setPunchOut(dayEnd);
            slot.setDurationMinutes(slotMinutes);
            slot.setAutoClosed(true);

            attendance.setTotalStudyMinutes(attendance.getTotalStudyMinutes() + slotMinutes);
            attendance.setShiftEnd(dayEnd);
            attendance.setStatus(AttendanceStatus.OUT);

            attendanceRepository.save(attendance);
            closedCount++;
        }

        log.info("Midnight auto punch-out sweep complete — closed {} forgotten session(s)", closedCount);

        // Every day is fully closed out by this point (either the student punched out
        // themselves, or the loop above just did it) -- so it's safe to drop yesterday's
        // (and any older) slot-level rows. The daily total they already contributed to
        // lives on in Attendance.totalStudyMinutes, so weekly/monthly stats are unaffected.
        long deletedSlots = attendanceSlotRepository.deleteByAttendance_AttendanceDateBefore(LocalDate.now());
        log.info("Midnight slot cleanup complete — removed {} slot row(s) from finished days", deletedSlots);
    }
}
