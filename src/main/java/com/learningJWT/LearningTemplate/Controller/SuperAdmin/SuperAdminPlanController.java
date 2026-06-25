package com.learningJWT.LearningTemplate.Controller.SuperAdmin;

import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryPlanDTO;
import com.learningJWT.LearningTemplate.Paylod.Response.ApiResponse;
import com.learningJWT.LearningTemplate.Services.LibraryPlanServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/superadmin/plan")
public class SuperAdminPlanController {

    private final LibraryPlanServices libraryPlanServices;
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping()
    public ResponseEntity<?> createPlan(@RequestBody LibraryPlanDTO libraryPlanDTO) {
        try {
            return ResponseEntity.ok(libraryPlanServices.createPlan(libraryPlanDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable Long id, @RequestBody LibraryPlanDTO libraryPlanDTO) {
        try {
            return ResponseEntity.ok(libraryPlanServices.updatePlan(id, libraryPlanDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<LibraryPlanDTO> getPlan(@PathVariable Long id ) throws Exception{
        return ResponseEntity.ok(libraryPlanServices.getPlan(id));
    }


    @PreAuthorize("hasRole('SUPERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deletePlan(@PathVariable Long id ) throws Exception{
        return ResponseEntity.ok(libraryPlanServices.deletePlan(id));
    }

    // NOTE: previously unguarded at the method level and relied on a permitAll() URL rule in
    // SecurityConfig that has since been removed — anyone could read full plan pricing with no
    // auth. Now properly gated both at the URL level (/api/superadmin/**) and here.
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping()
    public ResponseEntity<List<LibraryPlanDTO>> getAllPlan() throws Exception{
        return ResponseEntity.ok(libraryPlanServices.getAllPlans());
    }

    /** Plans currently offered to new/upgrading libraries (isActive = true only). */
    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/active")
    public ResponseEntity<List<LibraryPlanDTO>> getActivePlans() throws Exception{
        return ResponseEntity.ok(libraryPlanServices.getActivePlans());
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{id}/activate")
    public ResponseEntity<LibraryPlanDTO> activatePlan(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(libraryPlanServices.setPlanActive(id, true));
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<LibraryPlanDTO> deactivatePlan(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(libraryPlanServices.setPlanActive(id, false));
    }

}
