package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Model.Attendance;
import com.learningJWT.LearningTemplate.Paylod.DTO.AttendanceDTO;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Services.AttendanceServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceServices attendanceServices;


    @PostMapping("/student/punch")
    public ResponseEntity<AttendanceDTO> punch() throws Exception {
        return ResponseEntity.ok(attendanceServices.studentPunch(null));
    }

    @PostMapping("/student/qrpunch")
    public ResponseEntity<AttendanceDTO> punchWithQR(@RequestParam String qrValue) throws Exception {
        return ResponseEntity.ok(attendanceServices.studentPunch(qrValue));
    }


    @GetMapping("/student/{studentId}")
    public ResponseEntity<AttendanceDTO> getAttendanceByStudentIdAndDate(@PathVariable Long studentId, @RequestParam (required = true) LocalDate date) throws Exception {
        return ResponseEntity.ok(attendanceServices.getAttendanceByStudentIdAndDate(studentId,date));
    }


    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/library")
    public ResponseEntity<List<AttendanceDTO>> libraryAttendanceByDate(@RequestParam (required = true) LocalDate date) throws Exception {
        return ResponseEntity.ok(attendanceServices.libraryAttendanceByDate(date));
    }



// i think ye koi kaam ka nahin hata dena ya isko baad me badal dena ki date range me library me konsa student kitna present tha
    @GetMapping("/student/range")
    public ResponseEntity<List<AttendanceDTO>> attendanceByDateRange(@RequestParam LocalDate startDate,@RequestParam LocalDate endDate) throws Exception {
        return ResponseEntity.ok(attendanceServices.attendanceByDateRange(startDate,endDate));
    }

    @GetMapping("/student/month")
    public ResponseEntity<List<AttendanceDTO>> monthAttendanceByStudentId() throws Exception {
        return ResponseEntity.ok(attendanceServices.monthAttendanceByStudentId());
    }



}
