package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Paylod.DTO.FeeDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.PlanDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentDTO;
import com.learningJWT.LearningTemplate.Paylod.Response.ApiResponse;
import com.learningJWT.LearningTemplate.Services.FeeServices;
import com.learningJWT.LearningTemplate.Services.LibraryAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libraryadmin")
@RequiredArgsConstructor
public class LibraryAdminController {

    private final LibraryAdminService libraryAdminService;
    private final FeeServices feeServices;

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
}

