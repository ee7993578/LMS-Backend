package com.learningJWT.LearningTemplate.Controller.Global;

import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Model.QR;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryPlanDTO;
import com.learningJWT.LearningTemplate.Repository.QRRepository;
import com.learningJWT.LearningTemplate.Services.LibraryPlanServices;
import com.learningJWT.LearningTemplate.Services.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicController {

    private final LibraryPlanServices libraryPlanServices;
    private final SuperAdminService superAdminService;
    private final QRRepository qrRepository;
    @GetMapping("/plan")
    public ResponseEntity<List<LibraryPlanDTO>> getAllPlan() throws Exception{
        return ResponseEntity.ok(libraryPlanServices.getActivePlans());
    }



    @PostMapping("/create")
    public ResponseEntity<?> createLibrary(@RequestBody LibraryDTO dto) {
        try {
            LibraryDTO createdLibrary = superAdminService.createLibrary(dto);
            return ResponseEntity.ok(createdLibrary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    /**
     * PUBLIC: Resolve a QR code value.
     * Returns:
     *   type = "ATTENDANCE" → logged-in student should punch
     *   type = "REGISTRATION" → not a student; redirect to /register/{libraryCode}
     */
    @GetMapping("/qr/resolve/{qrValue}")
    public ResponseEntity<?> resolveQR(@PathVariable String qrValue) {
        try {
            // Find QR by value
            QR qr = qrRepository.findByQrCodeValueAndLibraryIdAndStatus(qrValue, null, Status.ACTIVE)
                    .orElse(null);
            // Try any-library search if above null
            if (qr == null) {
                qr = qrRepository.findAll().stream()
                        .filter(q -> q.getQrCodeValue() != null && q.getQrCodeValue().equals(qrValue))
                        .findFirst().orElse(null);
            }
            if (qr == null) return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired QR code"));

            String libraryCode = qr.getLibrary() != null ? qr.getLibrary().getLibraryCode() : null;
            boolean regEnabled = qr.getLibrary() != null && qr.getLibrary().isRegistrationEnabled();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("qrValue", qrValue);
            result.put("libraryId", qr.getLibrary() != null ? qr.getLibrary().getId() : null);
            result.put("libraryName", qr.getLibrary() != null ? qr.getLibrary().getName() : null);
            result.put("libraryCode", libraryCode);
            result.put("registrationEnabled", regEnabled);
            // type tells the scanner what to do
            result.put("type", "DUAL"); // frontend decides based on auth state
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
