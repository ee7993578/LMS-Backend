package com.learningJWT.LearningTemplate.Controller.Global;

import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Repository.SeatRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.StudentSelfRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StudentRegistrationController {

    private final StudentSelfRegistrationService registrationService;
    private final UserRepository userRepository;

    // ── PUBLIC: Validate library code (no auth) ──────────────────────────────
    @GetMapping("/api/public/register/validate/{code}")
    public ResponseEntity<?> validateCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(registrationService.validateLibraryCode(code));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── PUBLIC: Submit registration (no auth, multipart) ─────────────────────
    @PostMapping("/api/public/register/student")
    public ResponseEntity<?> register(
            @RequestParam("libraryCode") String libraryCode,
            @RequestParam("fullName") String fullName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "fatherName", required = false) String fatherName,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "aadharPhoto", required = false) MultipartFile aadharPhoto) {
        try {
            return ResponseEntity.ok(registrationService.submitRegistration(
                    libraryCode, fullName, phone, email, fatherName, address, photo, aadharPhoto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── ADMIN: Get pending registrations ─────────────────────────────────────
    @GetMapping("/api/libraryadmin/registrations/pending")
    public ResponseEntity<?> getPending() {
        try {
            Long libId = getAdminLibraryId();
            return ResponseEntity.ok(registrationService.getPendingRegistrations(libId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── ADMIN: Approve a student (assign seat + plan) ─────────────────────────
    @PostMapping("/api/libraryadmin/registrations/{studentId}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long studentId,
            @RequestBody Map<String, Object> body) {
        try {
            Long seatId = body.get("seatId") != null ? Long.parseLong(body.get("seatId").toString()) : null;
            Long planId = body.get("planId") != null ? Long.parseLong(body.get("planId").toString()) : null;
            String admNo = body.get("admissionNumber") != null ? body.get("admissionNumber").toString() : null;
            double fee = body.get("monthlyFee") != null ? Double.parseDouble(body.get("monthlyFee").toString()) : 0;
            String adminUsername = getCurrentUsername();
            return ResponseEntity.ok(registrationService.approveStudent(studentId, seatId, planId, admNo, fee, adminUsername));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── ADMIN: Reject a student ───────────────────────────────────────────────
    @PostMapping("/api/libraryadmin/registrations/{studentId}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long studentId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = (body != null) ? body.get("reason") : null;
            registrationService.rejectStudent(studentId, reason);
            return ResponseEntity.ok(Map.of("message", "Student registration rejected."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── ADMIN: Get / update registration settings ─────────────────────────────
    @GetMapping("/api/libraryadmin/registrations/settings")
    public ResponseEntity<?> getSettings() {
        try {
            return ResponseEntity.ok(registrationService.getSettings(getAdminLibraryId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/api/libraryadmin/registrations/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Object> body) {
        try {
            boolean enabled        = Boolean.parseBoolean(body.getOrDefault("registrationEnabled","true").toString());
            boolean requireApproval= Boolean.parseBoolean(body.getOrDefault("requireAdminApproval","true").toString());
            boolean regenerateCode = Boolean.parseBoolean(body.getOrDefault("regenerateCode","false").toString());
            return ResponseEntity.ok(registrationService.updateSettings(
                    getAdminLibraryId(), enabled, requireApproval, regenerateCode));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Long getAdminLibraryId() throws Exception {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("User not found"))
                .getLibrary().getId();
    }

    private String getCurrentUsername() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (p instanceof UserDetails ud) ? ud.getUsername() : p.toString();
    }
}
