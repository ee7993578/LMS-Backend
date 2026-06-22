package com.learningJWT.LearningTemplate.Controller.SuperAdmin;

import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Services.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/library")
@RequiredArgsConstructor
public class SuperAdminLibraryController {

    private final SuperAdminService superAdminService;

//    @PreAuthorize("hasRole('SUPERADMIN')")
    @PostMapping("/create")
    public ResponseEntity<?> createLibrary(@RequestBody LibraryDTO dto) {
        try {
            LibraryDTO createdLibrary = superAdminService.createLibrary(dto);
            return ResponseEntity.ok(createdLibrary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateLibrary(@PathVariable Long id, @RequestBody LibraryDTO dto) {
        try {
            LibraryDTO updatedLibrary = superAdminService.updateLibrary(id, dto);
            return ResponseEntity.ok(updatedLibrary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteLibrary(@PathVariable Long id) {
        try {
            superAdminService.deleteLibrary(id);
            return ResponseEntity.ok("Library deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }



    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping()
    public ResponseEntity<List<LibraryDTO>> getAllLibraries() {
         return ResponseEntity.ok(superAdminService.getAllLibraries());
    }




    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/{libraryId}")
    public ResponseEntity<LibraryDTO> findByLibraryId(@PathVariable Long libraryId) throws Exception {

        return ResponseEntity.ok(superAdminService.findById(libraryId));
    }


    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<LibraryDTO>> findByLibraryStatus(@PathVariable String status) throws Exception {
        return ResponseEntity.ok(superAdminService.findByStatus(status));
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/update/status/{id}")
    public ResponseEntity<LibraryDTO> changeLibraryStatus(@PathVariable Long id, @RequestParam String status) throws Exception {
        return ResponseEntity.ok(superAdminService.changeLibraryStatus(id,status));
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{libraryId}/plan/{planId}")
    public ResponseEntity<LibraryDTO> assignPlanToLibrary(@PathVariable Long libraryId, @PathVariable Long  planId) throws Exception {
        return ResponseEntity.ok(superAdminService.assignPlanToLibrary(libraryId,planId));
    }


}
