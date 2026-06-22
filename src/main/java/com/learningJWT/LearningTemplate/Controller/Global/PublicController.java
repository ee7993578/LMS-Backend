package com.learningJWT.LearningTemplate.Controller.Global;

import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryPlanDTO;
import com.learningJWT.LearningTemplate.Services.LibraryPlanServices;
import com.learningJWT.LearningTemplate.Services.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicController {

    private final   LibraryPlanServices libraryPlanServices;
    private final SuperAdminService superAdminService;
    @GetMapping("/plan")
    public ResponseEntity<List<LibraryPlanDTO>> getAllPlan() throws Exception{
        return ResponseEntity.ok(libraryPlanServices.getAllPlans());
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
}
