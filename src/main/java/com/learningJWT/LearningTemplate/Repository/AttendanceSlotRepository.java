package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.AttendanceSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceSlotRepository extends JpaRepository<AttendanceSlot, Long> {

    List<AttendanceSlot> findByAttendanceIdOrderBySlotNumberAsc(Long attendanceId);

    // Used by the midnight scheduler to find any slot a student forgot to punch out of.
    List<AttendanceSlot> findByPunchOutIsNull();

    // Used by the midnight scheduler to wipe slot-level detail for days that are already
    // finished. Attendance.totalStudyMinutes (used for weekly/monthly stats) is untouched --
    // only the per-slot rows for past days are removed.
    long deleteByAttendance_AttendanceDateBefore(LocalDate date);
}
