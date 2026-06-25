package com.learningJWT.LearningTemplate.Controller.SuperAdmin;

import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryUsageDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.SuperAdminDashboardDTO;
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

    // NOTE: this endpoint used to be permitAll() in SecurityConfig — that was a security hole
    // (anyone could create a library with no auth at all). It is now gated by both the
    // /api/superadmin/** URL rule (hasRole(SUPERADMIN)) AND this method-level annotation.
    @PreAuthorize("hasRole('SUPERADMIN')")
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

    /** Soft-delete only — see SuperAdminServiceImpl#deleteLibrary. Library, admin, and all
     *  student records are preserved; only the status flips to DELETED. */
    @PreAuthorize("hasRole('SUPERADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteLibrary(@PathVariable Long id) {
        try {
            superAdminService.deleteLibrary(id);
            return ResponseEntity.ok("Library marked as deleted (soft-delete; no data was removed)");
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

    // ===================== Dashboard =====================
    // (see SuperAdminDashboardController for GET /api/superadmin/dashboard)

    // ===================== Lifecycle actions =====================

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{libraryId}/activate")
    public ResponseEntity<?> activateLibrary(@PathVariable Long libraryId) {
        try {
            return ResponseEntity.ok(superAdminService.activateLibrary(libraryId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{libraryId}/suspend")
    public ResponseEntity<?> suspendLibrary(@PathVariable Long libraryId) {
        try {
            return ResponseEntity.ok(superAdminService.suspendLibrary(libraryId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{libraryId}/restore")
    public ResponseEntity<?> restoreLibrary(@PathVariable Long libraryId) {
        try {
            return ResponseEntity.ok(superAdminService.restoreLibrary(libraryId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Renews the library's subscription. Optional ?days= overrides the plan's default duration. */
    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{libraryId}/renew")
    public ResponseEntity<?> renewSubscription(@PathVariable Long libraryId,
                                                @RequestParam(required = false) Integer days) {
        try {
            return ResponseEntity.ok(superAdminService.renewSubscription(libraryId, days));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{libraryId}/upgrade/{newPlanId}")
    public ResponseEntity<?> upgradePlan(@PathVariable Long libraryId, @PathVariable Long newPlanId) {
        try {
            return ResponseEntity.ok(superAdminService.upgradePlan(libraryId, newPlanId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{libraryId}/downgrade/{newPlanId}")
    public ResponseEntity<?> downgradePlan(@PathVariable Long libraryId, @PathVariable Long newPlanId) {
        try {
            return ResponseEntity.ok(superAdminService.downgradePlan(libraryId, newPlanId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===================== Usage =====================

    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/{libraryId}/usage")
    public ResponseEntity<?> getLibraryUsage(@PathVariable Long libraryId) {
        try {
            return ResponseEntity.ok(superAdminService.getLibraryUsage(libraryId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
