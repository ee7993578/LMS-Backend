package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.AttendanceDTO;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceServices {

      AttendanceDTO studentPunch(String qrValue) throws Exception;

      List<AttendanceDTO> attendanceByDateRange(LocalDate startDate, LocalDate endDate) throws Exception;

    AttendanceDTO getAttendanceByStudentIdAndDate(Long studentId, LocalDate attendanceDate) throws Exception;

    List<AttendanceDTO> monthAttendanceByStudentId() throws Exception;


    List<AttendanceDTO> libraryAttendanceByDate(LocalDate date) throws Exception;
}
