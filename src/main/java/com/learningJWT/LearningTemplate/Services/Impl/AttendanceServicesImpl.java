package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Enum.AttendanceStatus;
import com.learningJWT.LearningTemplate.Enum.UserRole;
import com.learningJWT.LearningTemplate.Mapper.AttendanceMapper;
import com.learningJWT.LearningTemplate.Model.Attendance;
import com.learningJWT.LearningTemplate.Model.AttendanceSlot;
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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
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
        LocalDateTime now = LocalDateTime.now();

        Attendance attendance = attendanceRepository.findByStudentIdAndAttendanceDate(student.getId(), today);

        if (attendance == null) {
            // First punch of the day -> IN, opens Slot 1
            attendance = new Attendance();
            attendance.setStudent(student);
            attendance.setLibrary(student.getLibrary());
            attendance.setAttendanceDate(today);
            attendance.setShiftStart(now);
            attendance.setStatus(AttendanceStatus.IN);
            attendance.setTotalStudyMinutes(0L);
            attendance.setTotalBreakMinutes(0L);

            attendance.getSlots().add(
                    AttendanceSlot.builder()
                            .attendance(attendance)
                            .slotNumber(1)
                            .punchIn(now)
                            .build()
            );

        } else if (attendance.getStatus() == AttendanceStatus.IN) {
            // Punch OUT -> close whichever slot is currently open.
            // totalStudyMinutes already holds every earlier slot from today, so we only
            // ever ADD this slot's minutes to it -- the running total is never reset.
            AttendanceSlot openSlot = findOpenSlot(attendance);

            if (openSlot == null) {
                // Defensive fallback for legacy rows created before slots existed
                openSlot = AttendanceSlot.builder()
                        .attendance(attendance)
                        .slotNumber(attendance.getSlots().size() + 1)
                        .punchIn(attendance.getShiftStart())
                        .build();
                attendance.getSlots().add(openSlot);
            }

            long slotMinutes = Math.max(0, Duration.between(openSlot.getPunchIn(), now).toMinutes());
            openSlot.setPunchOut(now);
            openSlot.setDurationMinutes(slotMinutes);

            attendance.setTotalStudyMinutes(attendance.getTotalStudyMinutes() + slotMinutes);
            attendance.setShiftEnd(now);
            attendance.setStatus(AttendanceStatus.OUT);

        } else {
            // Punch IN again on the same day -> opens the next slot (Slot 2, Slot 3, ...).
            // The elapsed time between the previous punch-out and now is just a break;
            // it is tracked but never subtracted from -- or added to -- study time,
            // and the new slot's timer starts fresh from 0 while the DAY total keeps
            // whatever was already banked.
            List<AttendanceSlot> slots = attendance.getSlots();
            AttendanceSlot lastSlot = slots.isEmpty() ? null : slots.get(slots.size() - 1);
            if (lastSlot != null && lastSlot.getPunchOut() != null) {
                long breakMinutes = Math.max(0, Duration.between(lastSlot.getPunchOut(), now).toMinutes());
                attendance.setTotalBreakMinutes(attendance.getTotalBreakMinutes() + breakMinutes);
            }

            attendance.getSlots().add(
                    AttendanceSlot.builder()
                            .attendance(attendance)
                            .slotNumber(slots.size() + 1)
                            .punchIn(now)
                            .build()
            );

            attendance.setShiftStart(now); // start of the newest (current) slot
            attendance.setStatus(AttendanceStatus.IN);
        }

        attendanceRepository.save(attendance);
        return AttendanceMapper.toDTO(attendance);
    }

    private AttendanceSlot findOpenSlot(Attendance attendance) {
        AttendanceSlot open = null;
        for (AttendanceSlot slot : attendance.getSlots()) {
            if (slot.getPunchOut() == null) {
                open = slot; // keep the last one found, in case of stray data
            }
        }
        return open;
    }


    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<AttendanceDTO> monthAttendanceByStudentId() throws Exception {

            LocalDate today = LocalDate.now();
            LocalDate startDate = today.withDayOfMonth(1);
            List<AttendanceDTO> attendances = attendanceByDateRange(startDate,today);
            return attendances;
    }
}
