package com.learningJWT.LearningTemplate.Controller.Student;

import com.learningJWT.LearningTemplate.Mapper.FeeMapper;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.PlanDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.SeatDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentDTO;
import com.learningJWT.LearningTemplate.Repository.AttendanceRepository;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.PaymentProofService;
import com.learningJWT.LearningTemplate.Services.PaymentSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final PaymentSettingsService paymentSettingsService;
    private final PaymentProofService paymentProofService;

    private User getLoggedInUser() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new Exception("Logged-in user not found"));
        }
        throw new Exception("No authenticated user found");
    }

    /** GET /api/student/my-seat — returns the logged-in student's seat and plan info */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-seat")
    public ResponseEntity<?> getMySeat() throws Exception {
        User user = getLoggedInUser();
        Student student = studentRepository.findByUserId(user.getId());
        if (student == null) {
            return ResponseEntity.notFound().build();
        }

        // Build SeatDTO using builder (avoids no-args constructor issue)
        SeatDTO.SeatDTOBuilder builder = SeatDTO.builder();

        if (student.getSeat() != null) {
            builder.id(student.getSeat().getId())
                   .seatName(student.getSeat().getSeatName())
                   .location(student.getSeat().getLocation())
                   .status(student.getSeat().getStatus());
        }

        if (student.getPlan() != null) {
            PlanDTO planDTO = PlanDTO.builder()
                    .id(student.getPlan().getId())
                    .name(student.getPlan().getName())
                    .duration(student.getPlan().getDuration())
                    .price(student.getPlan().getPrice())
                    .build();
            builder.studentId(student.getId());
        }

        return ResponseEntity.ok(builder.build());
    }

    /** GET /api/student/my-fees — returns the logged-in student's fee records */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-fees")
    public ResponseEntity<?> getMyFees() throws Exception {
        User user = getLoggedInUser();
        Student student = studentRepository.findByUserId(user.getId());
        if (student == null) {
            return ResponseEntity.notFound().build();
        }
        List<FeeDTO> fees = student.getFees().stream()
                .map(FeeMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(fees);
    }

    /** GET /api/student/my-profile — returns student profile details */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile() throws Exception {
        User user = getLoggedInUser();
        Student student = studentRepository.findByUserId(user.getId());
        if (student == null) {
            return ResponseEntity.notFound().build();
        }
        StudentDTO dto = StudentDTO.builder()
                .id(student.getId())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .phone(student.getPhone())
                .username(user.getUsername())
                .build();
        return ResponseEntity.ok(dto);
    }

    /** GET /api/student/attendance-mode — returns library's attendance mode for this student */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/attendance-mode")
    public ResponseEntity<?> getAttendanceMode() throws Exception {
        User user = getLoggedInUser();
        Student student = studentRepository.findByUserId(user.getId());
        if (student == null || student.getLibrary() == null) {
            return ResponseEntity.ok(java.util.Map.of("attendanceMode", "BOTH"));
        }
        com.learningJWT.LearningTemplate.Enum.AttendanceMode mode = student.getLibrary().getAttendanceMode();
        return ResponseEntity.ok(java.util.Map.of("attendanceMode", mode != null ? mode.name() : "BOTH"));
    }

    /**
     * GET /api/student/leaderboard?period=month|week|all
     * Returns ranked list of students by study minutes for this library.
     */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(
            @RequestParam(defaultValue = "month") String period) throws Exception {

        User user = getLoggedInUser();
        Student currentStudent = studentRepository.findByUserId(user.getId());
        if (currentStudent == null) {
            return ResponseEntity.notFound().build();
        }

        List<Student> libraryStudents = studentRepository.findByLibrary(currentStudent.getLibrary());

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = switch (period) {
            case "week" -> endDate.minusWeeks(1);
            case "all"  -> LocalDate.of(2000, 1, 1);
            default     -> endDate.withDayOfMonth(1); // month
        };

        final LocalDate finalStart = startDate;
        final LocalDate finalEnd   = endDate;

        List<Map<String, Object>> leaderboard = libraryStudents.stream()
                .map(s -> {
                    long totalMinutes = attendanceRepository
                            .findByStudentIdAndAttendanceDateBetween(s.getId(), finalStart, finalEnd)
                            .stream()
                            .mapToLong(a -> a.getTotalStudyMinutes() != null ? a.getTotalStudyMinutes() : 0L)
                            .sum();

                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("studentId", s.getId());
                    entry.put("name",      s.getFullName());
                    entry.put("minutes",   totalMinutes);
                    entry.put("you",       s.getId().equals(currentStudent.getId()));
                    return entry;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("minutes"), (Long) a.get("minutes")))
                .collect(Collectors.toList());

        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).put("rank", i + 1);
        }

        return ResponseEntity.ok(leaderboard);
    }

    // ===================== Deposit (admin's QR/UPI/phone) =====================

    /** GET /api/student/payment-settings — shows the library's QR/UPI/phone/description for deposits */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/payment-settings")
    public ResponseEntity<?> getPaymentSettings() {
        try {
            return ResponseEntity.ok(paymentSettingsService.getSettingsForStudent());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================== Payment Proof =====================

    /** POST /api/student/payment-proof — submit a screenshot and/or description for a fee payment */
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(value = "/payment-proof", consumes = "multipart/form-data")
    public ResponseEntity<?> submitPaymentProof(
            @RequestParam(value = "screenshot", required = false) MultipartFile screenshot,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "amountClaimed", required = false) Double amountClaimed) {
        try {
            return ResponseEntity.ok(paymentProofService.submitProof(screenshot, description, amountClaimed));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** GET /api/student/payment-proofs — the logged-in student's own submission history */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/payment-proofs")
    public ResponseEntity<?> getMyPaymentProofs() {
        try {
            return ResponseEntity.ok(paymentProofService.getMyProofs());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
