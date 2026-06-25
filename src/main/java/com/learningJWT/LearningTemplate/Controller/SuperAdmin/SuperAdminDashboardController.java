package com.learningJWT.LearningTemplate.Controller.SuperAdmin;

import com.learningJWT.LearningTemplate.Services.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SuperAdmin Dashboard:
 *   GET /api/superadmin/dashboard       -> Total/Trial/Active/Expired/Inactive/Deleted libraries,
 *                                          total students, revenue summary
 *   GET /api/superadmin/usage           -> plan-usage (current count / limit / grace) for every library
 */
@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
public class SuperAdminDashboardController {

    private final SuperAdminService superAdminService;

    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            return ResponseEntity.ok(superAdminService.getDashboardStats());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping("/usage")
    public ResponseEntity<?> getAllLibraryUsage() {
        try {
            return ResponseEntity.ok(superAdminService.getAllLibraryUsage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
