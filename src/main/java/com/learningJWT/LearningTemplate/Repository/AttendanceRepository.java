package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.Attendance;
import com.learningJWT.LearningTemplate.Paylod.DTO.AttendanceDTO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance,Long> {

    Attendance findByStudentIdAndAttendanceDate(Long studentId, LocalDate attendanceDate);
    List<Attendance> findByStudentIdAndAttendanceDateBetween(Long studentId,LocalDate startDate,LocalDate endDate);
    List<Attendance> findByLibraryIdAndAttendanceDate(Long studentId,LocalDate date);
}
