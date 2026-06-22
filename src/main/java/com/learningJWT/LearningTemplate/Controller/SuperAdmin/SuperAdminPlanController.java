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
    public ResponseEntity<LibraryPlanDTO> createPlan(@RequestBody LibraryPlanDTO libraryPlanDTO) throws Exception{
        return ResponseEntity.ok(libraryPlanServices.createPlan(libraryPlanDTO));
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<LibraryPlanDTO> updatePlan(@PathVariable Long id ,@RequestBody LibraryPlanDTO libraryPlanDTO) throws Exception{
        return ResponseEntity.ok(libraryPlanServices.updatePlan(id,libraryPlanDTO));
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

//    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping()
    public ResponseEntity<List<LibraryPlanDTO>> getAllPlan() throws Exception{
        return ResponseEntity.ok(libraryPlanServices.getAllPlans());
    }

}
