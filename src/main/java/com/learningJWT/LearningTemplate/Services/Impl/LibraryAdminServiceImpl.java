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
import com.learningJWT.LearningTemplate.Repository.PlanRepository;
import com.learningJWT.LearningTemplate.Repository.QRRepository;
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
    public void deleteStudent(Long studentId) throws Exception {
        User admin = getLoggedInAdmin();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found"));

        if (!student.getLibrary().getId().equals(admin.getLibrary().getId())) {
            throw new Exception("You are not allowed to perform this action");
        }

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
        Plan savedPlan = planRepository.save(plan);
        return PlanMapper.toDto(savedPlan);
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
