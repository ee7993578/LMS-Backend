package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Paylod.DTO.FeeDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.PaymentProofDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.PaymentSettingsDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.PlanDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.PlanUpgradeRequestDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentDTO;
import com.learningJWT.LearningTemplate.Paylod.Response.ApiResponse;
import com.learningJWT.LearningTemplate.Services.FeeServices;
import com.learningJWT.LearningTemplate.Services.LibraryAdminService;
import com.learningJWT.LearningTemplate.Services.PaymentProofService;
import com.learningJWT.LearningTemplate.Services.PaymentSettingsService;
import com.learningJWT.LearningTemplate.Services.PlanUpgradeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/libraryadmin")
@RequiredArgsConstructor
public class LibraryAdminController {

    private final LibraryAdminService libraryAdminService;
    private final FeeServices feeServices;
    private final PaymentSettingsService paymentSettingsService;
    private final PaymentProofService paymentProofService;
    private final PlanUpgradeRequestService planUpgradeRequestService;

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PostMapping("/student")
    public ResponseEntity<?> createStudent(@RequestBody StudentDTO dto) {
        try {
            return ResponseEntity.ok(libraryAdminService.createStudent(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping("/student/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable Long id, @RequestBody StudentDTO dto) {
        try {
            return ResponseEntity.ok(libraryAdminService.updateStudent(id, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @DeleteMapping("/student/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        try {
            libraryAdminService.deleteStudent(id);
            return ResponseEntity.ok("Student deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/students")
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        return ResponseEntity.ok(libraryAdminService.getAllStudents());
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/subscription-status")
    public ResponseEntity<?> getMySubscriptionStatus() {
        try {
            return ResponseEntity.ok(libraryAdminService.getMySubscriptionStatus());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================== Plan upgrade requests =====================
    // Library admin browses all plans (see /plan/catalog below), picks one, and submits a
    // request here. SuperAdmin then approves (actually applies the plan change) or rejects it
    // from the SuperAdmin Plan Requests page.

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/plan/catalog")
    public ResponseEntity<?> getPlanCatalog() {
        // All active plans the library admin can choose to request an upgrade/downgrade to.
        try {
            return ResponseEntity.ok(libraryAdminService.getPlanCatalog());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PostMapping("/plan-requests")
    public ResponseEntity<?> requestPlanChange(@RequestBody com.learningJWT.LearningTemplate.Paylod.DTO.PlanChangeRequestBody body) {
        try {
            return ResponseEntity.ok(planUpgradeRequestService.createRequest(body.getRequestedPlanId(), body.getNote()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/plan-requests")
    public ResponseEntity<?> getMyPlanRequests() {
        try {
            return ResponseEntity.ok(planUpgradeRequestService.getMyRequests());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PostMapping("/plan")
    public ResponseEntity<PlanDTO> createPlan(@RequestBody PlanDTO planDTO) throws Exception {
        return ResponseEntity.ok(libraryAdminService.createPlan(planDTO));
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/plan/{id}")
    public ResponseEntity<PlanDTO> getById(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(libraryAdminService.getPlanById(id));
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping("/plan/{id}")
    public ResponseEntity<PlanDTO> updatePlan(@PathVariable Long id, @RequestBody PlanDTO planDTO) throws Exception {
        return ResponseEntity.ok(libraryAdminService.updatePlan(id, planDTO));
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @DeleteMapping("/plan/{id}")
    public ResponseEntity<ApiResponse> deletePlan(@PathVariable Long id) throws Exception {
        libraryAdminService.deletePlan(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Plan deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/plan")
    public ResponseEntity<List<PlanDTO>> getAllPlan() throws Exception {
        return ResponseEntity.ok(libraryAdminService.getAllPlans());
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping("/fee/{studentId}")
    public ResponseEntity<FeeDTO> updateFeeStudent(@PathVariable Long studentId, @RequestBody FeeDTO feeDTO) throws Exception {
        return ResponseEntity.ok(feeServices.updateFee(studentId, feeDTO));
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/fee")
    public ResponseEntity<List<FeeDTO>> getLibraryFee() throws Exception {
        return ResponseEntity.ok(feeServices.getFeeByLibraryId());
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/fee/{monthId}")
    public ResponseEntity<List<FeeDTO>> getLibraryFeeByMonthId(@PathVariable int monthId) throws Exception {
        return ResponseEntity.ok(feeServices.findByMonthIdLibraryId(monthId));
    }

    /**
     * Subscription Expiry Report:
     * Returns students grouped into:
     * - "expired": subscriptionExpiryDate < today (already expired), with daysAgo
     * - "expiringSoon": today <= subscriptionExpiryDate <= today+7
     */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/fee/subscription-report")
    public ResponseEntity<?> getSubscriptionExpiryReport() {
        try {
            List<StudentDTO> students = libraryAdminService.getAllStudents();
            LocalDate today = LocalDate.now();
            LocalDate in7Days = today.plusDays(7);

            List<Map<String, Object>> expired = new ArrayList<>();
            List<Map<String, Object>> expiringSoon = new ArrayList<>();

            for (StudentDTO s : students) {
                LocalDate expiry = s.getSubscriptionExpiryDate();
                if (expiry == null) continue;

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("studentId", s.getId());
                entry.put("fullName", s.getFullName());
                entry.put("phone", s.getPhone());
                entry.put("planName", s.getPlan() != null ? s.getPlan().getName() : null);
                entry.put("dateOfJoin", s.getDateOfJoin());
                entry.put("expiryDate", expiry);

                if (expiry.isBefore(today)) {
                    long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(expiry, today);
                    entry.put("daysAgo", daysAgo);
                    expired.add(entry);
                } else if (!expiry.isAfter(in7Days)) {
                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, expiry);
                    entry.put("daysLeft", daysLeft);
                    expiringSoon.add(entry);
                }
            }

            // Sort expired: most recently expired first (smallest daysAgo first)
            expired.sort((a, b) -> Long.compare((Long) a.get("daysAgo"), (Long) b.get("daysAgo")));
            // Sort expiringSoon: soonest expiry first
            expiringSoon.sort((a, b) -> Long.compare((Long) a.get("daysLeft"), (Long) b.get("daysLeft")));

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("generatedAt", today.toString());
            report.put("expiredCount", expired.size());
            report.put("expiringSoonCount", expiringSoon.size());
            report.put("expired", expired);
            report.put("expiringSoon", expiringSoon);

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/generate")
    public ResponseEntity<?> generateQR() throws Exception {
        String base64 = libraryAdminService.generateQR();
        return ResponseEntity.ok("data:image/png;base64," + base64);
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/getQR")
    public ResponseEntity<?> getQR() throws Exception {
        try {
            String base64 = libraryAdminService.getQR();
            return ResponseEntity.ok("data:image/png;base64," + base64);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ===================== Payment (deposit QR/UPI/phone) =====================

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/payment-settings")
    public ResponseEntity<?> getPaymentSettings() {
        try {
            return ResponseEntity.ok(paymentSettingsService.getMySettings());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PostMapping(value = "/payment-settings", consumes = "multipart/form-data")
    public ResponseEntity<?> savePaymentSettings(
            @RequestParam(value = "qrFile", required = false) MultipartFile qrFile,
            @RequestParam(value = "upiId", required = false) String upiId,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "description", required = false) String description) {
        try {
            PaymentSettingsDTO saved = paymentSettingsService.saveSettings(qrFile, upiId, phoneNumber, description);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================== Payment Proof Verification =====================

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/payment-proofs")
    public ResponseEntity<?> getAllPaymentProofs() {
        try {
            return ResponseEntity.ok(paymentProofService.getLibraryProofs());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/payment-proofs/pending")
    public ResponseEntity<?> getPendingPaymentProofs() {
        try {
            return ResponseEntity.ok(paymentProofService.getPendingProofs());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Admin verifies a proof. Body is the same FeeDTO shape used by PUT /fee/{studentId} —
     * Receive, concession, lateFee, feeStatus etc. The fee record is updated using the
     * exact same logic as a manual fee deposit, then the proof is marked VERIFIED.
     */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping("/payment-proofs/{proofId}/verify")
    public ResponseEntity<?> verifyPaymentProof(@PathVariable Long proofId, @RequestBody FeeDTO feeDTO) {
        try {
            return ResponseEntity.ok(paymentProofService.verifyProof(proofId, feeDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping("/payment-proofs/{proofId}/reject")
    public ResponseEntity<?> rejectPaymentProof(@PathVariable Long proofId, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(paymentProofService.rejectProof(proofId, body.get("adminNote")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
