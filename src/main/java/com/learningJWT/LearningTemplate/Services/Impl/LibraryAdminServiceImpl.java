package com.learningJWT.LearningTemplate.Services.Impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Enum.UserRole;
import com.learningJWT.LearningTemplate.Mapper.PlanMapper;
import com.learningJWT.LearningTemplate.Mapper.StudentMapper;
import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Paylod.DTO.PlanDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentDTO;
import com.learningJWT.LearningTemplate.Enum.SeatStatus;
import com.learningJWT.LearningTemplate.Repository.PlanRepository;
import com.learningJWT.LearningTemplate.Repository.QRRepository;
import com.learningJWT.LearningTemplate.Repository.SeatAllocationRepository;
import com.learningJWT.LearningTemplate.Repository.SeatRepository;
import com.learningJWT.LearningTemplate.Repository.SlotRepository;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.LibraryAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LibraryAdminServiceImpl implements LibraryAdminService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final QRRepository qrRepository;
    private final PlanRepository planRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final SeatRepository seatRepository;
    private final SlotRepository slotRepository;

    private User getLoggedInAdmin() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new Exception("Logged-in user not found"));
        }
        throw new Exception("No authenticated user found");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentDTO createStudent(StudentDTO dto) throws Exception {
        User admin = getLoggedInAdmin();
        Library library = admin.getLibrary();

        if (library == null) {
            throw new Exception("Admin is not associated with a library");
        }

        String loginUsername = dto.getUsername() != null && !dto.getUsername().isBlank()
                ? dto.getUsername()
                : dto.getEmail(); // fallback to email if no username provided

        if (userRepository.existsByUsername(loginUsername)) {
            throw new Exception("Username already exists: " + loginUsername);
        }

        User studentUser = new User();
        studentUser.setUsername(loginUsername);
        studentUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        studentUser.setRole(UserRole.ROLE_STUDENT);
        studentUser.setLibrary(library);
        studentUser.setFullName(dto.getFullName());
        studentUser.setPhone(dto.getPhone());
        studentUser.setCreatedAt(LocalDateTime.now());
        studentUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(studentUser);

        Student student = StudentMapper.toEntity(dto);
        student.setLibrary(library);
        student.setUser(studentUser);
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        // Set date of join — use provided date or default to today
        student.setDateOfJoin(dto.getDateOfJoin() != null ? dto.getDateOfJoin() : java.time.LocalDate.now());
        studentRepository.save(student);

        return StudentMapper.toDTO(student);
    }

    @Override
    public StudentDTO updateStudent(Long studentId, StudentDTO dto) throws Exception {
        User admin = getLoggedInAdmin();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found"));

        if (!student.getLibrary().getId().equals(admin.getLibrary().getId())) {
            throw new Exception("You are not allowed to perform this action");
        }

        student.setFullName(dto.getFullName());
        student.setEmail(dto.getEmail());
        student.setPhone(dto.getPhone());
        student.setUpdatedAt(LocalDateTime.now());
        studentRepository.save(student);

        return StudentMapper.toDTO(student);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStudent(Long studentId) throws Exception {
        User admin = getLoggedInAdmin();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found"));

        if (!student.getLibrary().getId().equals(admin.getLibrary().getId())) {
            throw new Exception("You are not allowed to perform this action");
        }

        // Deallocate all active seat allocations and free up seat if needed
        List<SeatAllocation> activeAllocs = seatAllocationRepository.findByStudentIdAndActiveTrue(studentId);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (SeatAllocation alloc : activeAllocs) {
            alloc.setActive(false);
            alloc.setDeallocatedAt(now);
            seatAllocationRepository.save(alloc);
            // Check if the seat has other active allocations from different students
            Seat seat = alloc.getSeat();
            boolean othersActive = seatAllocationRepository.findBySeatIdAndActiveTrue(seat.getId())
                    .stream().anyMatch(a -> !a.getStudent().getId().equals(studentId));
            if (!othersActive) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            }
        }

        // Clear student's seat reference
        student.setSeat(null);
        studentRepository.save(student);

        studentRepository.delete(student);
    }

    @Override
    public List<StudentDTO> getAllStudents() {
        try {
            User admin = getLoggedInAdmin();
            Library library = admin.getLibrary();
            return studentRepository.findByLibrary(library)
                    .stream()
                    .map(StudentMapper::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public PlanDTO createPlan(PlanDTO dto) throws Exception {
        User user = getLoggedInAdmin();
        Plan plan = new Plan();
        plan.setName(dto.getName());
        plan.setDuration(dto.getDuration());
        plan.setLibrary(user.getLibrary());
        plan.setPrice(dto.getPrice());
        plan.setHoursPerDay(dto.getHoursPerDay());
        plan.setStudyDays(dto.getStudyDays());
        plan.setSubscriptionDays(dto.getSubscriptionDays());
        Plan savedPlan = planRepository.save(plan);

        // Auto-generate slots if hoursPerDay is set and evenly divides 24
        int hoursPerDay = savedPlan.getHoursPerDay() != null ? savedPlan.getHoursPerDay()
                : (savedPlan.getDuration() != null ? savedPlan.getDuration().intValue() : 0);
        if (hoursPerDay > 0 && 24 % hoursPerDay == 0) {
            autoGenerateSlotsForPlan(savedPlan, user.getLibrary(), hoursPerDay);
        }

        return PlanMapper.toDto(savedPlan);
    }

    private void autoGenerateSlotsForPlan(Plan plan, Library library, int hoursPerDay) {
        int totalSlots = 24 / hoursPerDay;
        String[] slotNames = {"Morning", "Afternoon", "Evening", "Night",
                              "Slot 5", "Slot 6", "Slot 7", "Slot 8"};
        LocalTime current = LocalTime.of(6, 0); // Start at 6 AM

        for (int i = 0; i < totalSlots; i++) {
            LocalTime start = current;
            LocalTime end = start.plusHours(hoursPerDay);
            // midnight: LocalTime.plusHours wraps around automatically (e.g. 18+6 = 00:00)

            String name = i < slotNames.length ? slotNames[i] : ("Slot " + (i + 1));
            Slot slot = Slot.builder()
                    .slotName(name)
                    .startTime(start)
                    .endTime(end)
                    .durationHours(hoursPerDay)
                    .library(library)
                    .plan(plan)
                    .build();
            slotRepository.save(slot);
            current = end;
        }
    }

    @Override
    public PlanDTO updatePlan(Long planId, PlanDTO dto) throws Exception {
        User admin = getLoggedInAdmin();
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new Exception("Plan not found"));

        if (!plan.getLibrary().getId().equals(admin.getLibrary().getId())) {
            throw new Exception("You are not allowed to perform this action");
        }

        plan.setName(dto.getName());
        plan.setPrice(dto.getPrice());
        plan.setDuration(dto.getDuration());
        plan.setHoursPerDay(dto.getHoursPerDay());
        plan.setStudyDays(dto.getStudyDays());
        plan.setSubscriptionDays(dto.getSubscriptionDays());
        return PlanMapper.toDto(planRepository.save(plan));
    }

    @Override
    public void deletePlan(Long planId) throws Exception {
        User admin = getLoggedInAdmin();
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new Exception("Plan not found"));

        if (!plan.getLibrary().getId().equals(admin.getLibrary().getId())) {
            throw new Exception("You are not allowed to perform this action");
        }

        planRepository.delete(plan);
    }

    @Override
    public List<PlanDTO> getAllPlans() throws Exception {
        User admin = getLoggedInAdmin();
        List<Plan> plans = planRepository.findByLibraryId(admin.getLibrary().getId());
        return plans.stream().map(PlanMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public PlanDTO getPlanById(Long id) throws Exception {
        User admin = getLoggedInAdmin();
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new Exception("Plan not found"));

        if (!plan.getLibrary().getId().equals(admin.getLibrary().getId())) {
            throw new Exception("You are not allowed to perform this action");
        }

        return PlanMapper.toDto(plan);
    }

    @Override
    public String generateQR() throws Exception {
        User user = getLoggedInAdmin();
        Long libraryId = user.getLibrary().getId();
        QR qr = qrRepository.findByLibraryId(libraryId);
        QR savedQR;
        if (qr != null) {
            qr.setQrCodeValue("library" + libraryId + "-" + UUID.randomUUID());
            qr.setStatus(Status.ACTIVE);
            savedQR = qrRepository.save(qr);
        } else {
            QR newQR = new QR();
            newQR.setQrCodeValue("library" + libraryId + "-" + UUID.randomUUID());
            newQR.setLibrary(user.getLibrary());
            newQR.setStatus(Status.ACTIVE);
            savedQR = qrRepository.save(newQR);
        }
        return generateQRImage(savedQR.getQrCodeValue());
    }

    @Override
    public String getQR() throws Exception {
        User user = getLoggedInAdmin();
        Long libraryId = user.getLibrary().getId();
        QR qr = qrRepository.findByLibraryId(libraryId);
        if (qr != null) {
            return generateQRImage(qr.getQrCodeValue());
        } else {
            throw new Exception("QR not found. Please generate one first.");
        }
    }

    private String generateQRImage(String text) throws Exception {
        int width = 250;
        int height = 250;
        BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
