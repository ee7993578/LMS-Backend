package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Enum.NotificationType;
import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Enum.UserRole;
import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Repository.*;
import com.learningJWT.LearningTemplate.Services.Impl.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentSelfRegistrationService {

    private final LibraryRepository libraryRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final LibraryLifecycleService lifecycleService;

    // ── Validate library code before showing form ───────────────────────────
    public Map<String, Object> validateLibraryCode(String code) throws Exception {
        Library lib = libraryRepository.findByLibraryCode(code.trim().toUpperCase())
                .orElseThrow(() -> new Exception("Invalid library code. Please check and try again."));

        if (!lib.isRegistrationEnabled())
            throw new Exception("Self-registration is currently disabled for this library.");

        Status s = lib.getStatus();
        if (s == Status.DELETED || s == Status.INACTIVE)
            throw new Exception("This library is currently inactive. Please contact the library admin.");

        if (s == Status.EXPIRED_READ_ONLY || s == Status.EXPIRED)
            throw new Exception("This library's subscription has expired. Please contact the library admin.");

        // Check plan student limit
        if (!lifecycleService.canRegisterNewStudent(lib))
            throw new Exception("This library has reached its student limit. Please contact the library admin.");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("libraryId", lib.getId());
        result.put("libraryName", lib.getName());
        result.put("libraryAddress", lib.getAddress());
        result.put("requireAdminApproval", lib.isRequireAdminApproval());
        return result;
    }

    // ── Submit self-registration ────────────────────────────────────────────
    @Transactional
    public Map<String, Object> submitRegistration(
            String libraryCode,
            String fullName,
            String phone,
            String email,
            String fatherName,
            String address,
            MultipartFile photo,
            MultipartFile aadharPhoto) throws Exception {

        Library lib = libraryRepository.findByLibraryCode(libraryCode.trim().toUpperCase())
                .orElseThrow(() -> new Exception("Invalid library code."));

        // Re-check all conditions
        validateLibraryCode(libraryCode);

        // Duplicate phone check
        if (phone != null && !phone.isBlank()
                && studentRepository.existsByPhoneAndLibraryId(phone.trim(), lib.getId()))
            throw new Exception("A student with this phone number is already registered in this library.");

        // Duplicate email check
        if (email != null && !email.isBlank()
                && studentRepository.existsByEmailAndLibraryId(email.trim(), lib.getId()))
            throw new Exception("A student with this email is already registered in this library.");

        // Upload files if provided
        String photoUrl = null, aadharUrl = null;
        if (photo != null && !photo.isEmpty()) photoUrl = fileStorageService.storeFile(photo);
        if (aadharPhoto != null && !aadharPhoto.isEmpty()) aadharUrl = fileStorageService.storeFile(aadharPhoto);

        // Generate temp username & password
        String tempUsername = generateUsername(fullName, lib.getId());
        String tempPassword = generateTempPassword();

        // Create User account (inactive until approved if requireAdminApproval)
        User user = new User();
        user.setUsername(tempUsername);
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setRole(UserRole.ROLE_STUDENT);
        user.setLibrary(lib);
        user.setFullName(fullName.trim());
        user.setPhone(phone != null ? phone.trim() : null);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Create Student record
        Student student = new Student();
        student.setFullName(fullName.trim());
        student.setPhone(phone != null ? phone.trim() : null);
        student.setEmail(email != null ? email.trim() : null);
        student.setFatherName(fatherName != null ? fatherName.trim() : null);
        student.setAddress(address != null ? address.trim() : null);
        student.setPhotoUrl(photoUrl);
        student.setAadharPhotoUrl(aadharUrl);
        student.setLibrary(lib);
        student.setUser(user);
        student.setActive(false); // inactive until approved
        student.setDateOfJoin(LocalDate.now());
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        student.setRegistrationStatus(
                lib.isRequireAdminApproval() ? "PENDING_APPROVAL" : "APPROVED"
        );

        if (!lib.isRequireAdminApproval()) {
            // Auto-approve
            student.setActive(true);
            student.setApprovedAt(LocalDateTime.now());
        }

        studentRepository.save(student);

        // Notify admin
        if (lib.getAdmin() != null) {
            notificationService.send(lib.getAdmin(), lib,
                    NotificationType.SYSTEM,
                    "New Student Registration",
                    fullName + " has submitted a registration request. Review in Students → Pending.",
                    "/admin/students");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("studentId", student.getId());
        response.put("requireAdminApproval", lib.isRequireAdminApproval());
        response.put("message", lib.isRequireAdminApproval()
                ? "Registration submitted! Please wait for admin approval. You will receive your login details once approved."
                : "Registration successful! Your login details have been sent.");
        // Only expose credentials if auto-approved
        if (!lib.isRequireAdminApproval()) {
            response.put("username", tempUsername);
            response.put("tempPassword", tempPassword);
        }
        return response;
    }

    // ── Admin: get all pending registrations ────────────────────────────────
    public List<Map<String, Object>> getPendingRegistrations(Long libraryId) {
        return studentRepository.findPendingByLibrary(libraryId)
                .stream().map(this::toSummary).toList();
    }

    // ── Admin: approve a student — assign seat + plan ────────────────────────
    @Transactional
    public Map<String, Object> approveStudent(Long studentId, Long seatId, Long planId,
                                               String admissionNumber, double monthlyFee,
                                               String adminUsername) throws Exception {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found"));

        if (!"PENDING_APPROVAL".equals(student.getRegistrationStatus()))
            throw new Exception("Student is not in PENDING_APPROVAL state");

        // Assign seat
        Seat seat = null;
        if (seatId != null) {
            seat = seatRepository.findById(seatId).orElse(null);
            if (seat != null) {
                seat.setStatus(com.learningJWT.LearningTemplate.Enum.SeatStatus.ALLOCATED);
                seatRepository.save(seat);
                student.setSeat(seat);
            }
        }

        // Assign plan
        if (planId != null) {
            planRepository.findById(planId).ifPresent(student::setPlan);
        }

        if (admissionNumber != null && !admissionNumber.isBlank())
            student.setAdmissionNumber(admissionNumber.trim());
        else
            student.setAdmissionNumber(generateAdmissionNumber(student.getLibrary().getId()));

        student.setRegistrationStatus("APPROVED");
        student.setActive(true);
        student.setApprovedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        studentRepository.save(student);

        // Generate temp credentials message
        String tempPassword = generateTempPassword();
        student.getUser().setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(student.getUser());

        // Notify student
        notificationService.send(student.getUser(), student.getLibrary(),
                NotificationType.SYSTEM,
                "Registration Approved! 🎉",
                String.format("Your registration at %s has been approved. Seat: %s | Admission: %s",
                        student.getLibrary().getName(),
                        seat != null ? seat.getSeatName() : "Not assigned yet",
                        student.getAdmissionNumber()),
                "/student/dashboard");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", student.getId());
        result.put("username", student.getUser().getUsername());
        result.put("tempPassword", tempPassword);
        result.put("admissionNumber", student.getAdmissionNumber());
        result.put("seatName", seat != null ? seat.getSeatName() : null);
        result.put("message", "Student approved and activated.");
        return result;
    }

    // ── Admin: reject a student ──────────────────────────────────────────────
    @Transactional
    public void rejectStudent(Long studentId, String reason) throws Exception {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found"));

        student.setRegistrationStatus("REJECTED");
        student.setRejectionReason(reason);
        student.setActive(false);
        student.setUpdatedAt(LocalDateTime.now());
        studentRepository.save(student);

        // Notify student
        if (student.getUser() != null) {
            notificationService.send(student.getUser(), student.getLibrary(),
                    NotificationType.SYSTEM,
                    "Registration Update",
                    "Your registration request was not approved. Reason: "
                            + (reason != null ? reason : "Please contact the library admin."),
                    null);
        }
    }

    // ── Admin: update registration settings ─────────────────────────────────
    @Transactional
    public Map<String, Object> updateSettings(Long libraryId, boolean enabled,
                                               boolean requireApproval,
                                               boolean regenerateCode) {
        Library lib = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));
        lib.setRegistrationEnabled(enabled);
        lib.setRequireAdminApproval(requireApproval);
        if (regenerateCode || lib.getLibraryCode() == null)
            lib.setLibraryCode(generateLibraryCode());
        libraryRepository.save(lib);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("libraryCode", lib.getLibraryCode());
        result.put("registrationEnabled", lib.isRegistrationEnabled());
        result.put("requireAdminApproval", lib.isRequireAdminApproval());
        result.put("registrationLink", "/register/" + lib.getLibraryCode());
        return result;
    }

    // ── Get current settings + pending count ────────────────────────────────
    public Map<String, Object> getSettings(Long libraryId) {
        Library lib = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));

        // Auto-generate code if missing
        if (lib.getLibraryCode() == null) {
            lib.setLibraryCode(generateLibraryCode());
            libraryRepository.save(lib);
        }

        long pendingCount = studentRepository.countByLibraryIdAndRegistrationStatus(
                libraryId, "PENDING_APPROVAL");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("libraryCode", lib.getLibraryCode());
        result.put("registrationEnabled", lib.isRegistrationEnabled());
        result.put("requireAdminApproval", lib.isRequireAdminApproval());
        result.put("registrationLink", "/register/" + lib.getLibraryCode());
        result.put("pendingCount", pendingCount);
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private String generateLibraryCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder("LIB-");
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private String generateUsername(String fullName, Long libId) {
        String base = fullName.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (base.length() > 8) base = base.substring(0, 8);
        String candidate = base + libId + new Random().nextInt(999);
        // ensure uniqueness
        while (userRepository.existsByUsername(candidate))
            candidate = base + libId + new Random().nextInt(9999);
        return candidate;
    }

    private String generateAdmissionNumber(Long libId) {
        return "ADM-" + libId + "-" + System.currentTimeMillis() % 100000;
    }

    private Map<String, Object> toSummary(Student s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("fullName", s.getFullName());
        m.put("phone", s.getPhone());
        m.put("email", s.getEmail());
        m.put("fatherName", s.getFatherName());
        m.put("address", s.getAddress());
        m.put("photoUrl", s.getPhotoUrl());
        m.put("aadharPhotoUrl", s.getAadharPhotoUrl());
        m.put("registrationStatus", s.getRegistrationStatus());
        m.put("createdAt", s.getCreatedAt());
        m.put("rejectionReason", s.getRejectionReason());
        return m;
    }
}
