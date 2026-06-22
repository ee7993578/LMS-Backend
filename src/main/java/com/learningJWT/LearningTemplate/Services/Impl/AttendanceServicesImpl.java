package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Enum.AttendanceStatus;
import com.learningJWT.LearningTemplate.Enum.UserRole;
import com.learningJWT.LearningTemplate.Mapper.AttendanceMapper;
import com.learningJWT.LearningTemplate.Model.Attendance;
import com.learningJWT.LearningTemplate.Model.QR;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.AttendanceDTO;
import com.learningJWT.LearningTemplate.Repository.AttendanceRepository;
import com.learningJWT.LearningTemplate.Repository.QRRepository;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.AttendanceServices;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServicesImpl implements AttendanceServices {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final QRRepository qrRepository;

    private User getLoggedInUser() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new Exception("Logged-in user not found"));
        }
        throw new Exception("No authenticated user found");
    }

    @Override
    public AttendanceDTO studentPunch(String qrValue) throws Exception {

        User user = getLoggedInUser();
        if (!user.getRole().equals(UserRole.ROLE_STUDENT)) {
            throw new Exception("Only students can punch");
        }

        if (qrValue!=null){
            QR qr = qrRepository.findByLibraryId(user.getLibrary().getId());
            if (qr==null || !qr.getQrCodeValue().equals(qrValue)){
                  throw new Exception("QR code not found");
            }
        }

        Student student = studentRepository.findByUserId(user.getId());



        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByStudentIdAndAttendanceDate(student.getId(), today);

        if (attendance == null) {
            // First punch of the day -> IN
            attendance = new Attendance();
            attendance.setStudent(student);
            attendance.setLibrary(student.getLibrary());
            attendance.setAttendanceDate(today);
            attendance.setShiftStart(LocalDateTime.now());
            attendance.setStatus(AttendanceStatus.IN);
            attendance.setTotalStudyMinutes(0L);
            attendance.setTotalBreakMinutes(0L);
        } else {
            if (attendance.getStatus() == AttendanceStatus.IN) {
                // Punch OUT
                LocalDateTime now = LocalDateTime.now();
                long studyMinutes = Duration.between(attendance.getShiftStart(), now).toMinutes()
                        - attendance.getTotalBreakMinutes();
                attendance.setTotalStudyMinutes(attendance.getTotalStudyMinutes() + studyMinutes);
                attendance.setShiftEnd(now);
                attendance.setStatus(AttendanceStatus.OUT);
            } else {
                // Punch IN after break
                LocalDateTime now = LocalDateTime.now();
                long breakMinutes = Duration.between(attendance.getShiftEnd(), now).toMinutes();
                attendance.setTotalBreakMinutes(attendance.getTotalBreakMinutes() + breakMinutes);

                attendance.setShiftStart(now);  // new shift start
                attendance.setStatus(AttendanceStatus.IN);
            }
        }

        attendanceRepository.save(attendance);
        return AttendanceMapper.toDTO(attendance);
    }


    public List<AttendanceDTO> libraryAttendanceByDate(LocalDate date) throws Exception {
        User user = getLoggedInUser();
        if (user.getRole().equals(UserRole.ROLE_LIBRARY_ADMIN)) {
            List<Attendance> attendances = attendanceRepository.findByLibraryIdAndAttendanceDate(user.getLibrary().getId(), date);

            return attendances.stream().map(AttendanceMapper::toDTO).collect(Collectors.toList());
        }else{
            throw new Exception("Only library admin can check date attendance");
        }
    }

    // change karna h baad me ki is mahine me kitne student present the or kitni absent student ki is month me
    @Override
    public List<AttendanceDTO> attendanceByDateRange(LocalDate startDate, LocalDate endDate) throws Exception {

        if (startDate == null) startDate = LocalDate.now();
        if (endDate == null) endDate = LocalDate.now();

        User user = getLoggedInUser();

        List<Attendance> attendances;

        if (user.getRole() == UserRole.ROLE_STUDENT) {
            Student student = studentRepository.findByUserId(user.getId());
            attendances = attendanceRepository.findByStudentIdAndAttendanceDateBetween(
                    student.getId(), startDate, endDate);
        } else {
            throw new Exception("Unauthorized");
        }

        return attendances.stream()
                .map(AttendanceMapper::toDTO)
                .collect(Collectors.toList());
    }


    @Override
    public AttendanceDTO getAttendanceByStudentIdAndDate(Long studentId, LocalDate attendanceDate) throws Exception {

        Attendance attendance = attendanceRepository.findByStudentIdAndAttendanceDate(studentId,attendanceDate);

        if (attendance !=null){
            return AttendanceMapper.toDTO(attendance);
        }
        else {
            throw new Exception("Attendance not found");
        }
    }

    @Override
    public List<AttendanceDTO> monthAttendanceByStudentId() throws Exception {

            LocalDate today = LocalDate.now();
            LocalDate startDate = today.withDayOfMonth(1);
            List<AttendanceDTO> attendances = attendanceByDateRange(startDate,today);
            return attendances;
    }
}
