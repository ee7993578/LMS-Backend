package com.learningJWT.LearningTemplate.Controller.SuperAdmin;

import com.learningJWT.LearningTemplate.Services.PlanUpgradeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SuperAdmin Plan Requests:
 *   GET  /api/superadmin/plan-requests              -> all requests (optionally ?status=PENDING)
 *   PUT  /api/superadmin/plan-requests/{id}/approve  -> approve (applies the plan change)
 *   PUT  /api/superadmin/plan-requests/{id}/reject   -> reject (library plan untouched)
 */
@RestController
@RequestMapping("/api/superadmin/plan-requests")
@RequiredArgsConstructor
public class SuperAdminPlanRequestController {

    private final PlanUpgradeRequestService planUpgradeRequestService;

    @PreAuthorize("hasRole('SUPERADMIN')")
    @GetMapping
    public ResponseEntity<?> getAllRequests(@RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(planUpgradeRequestService.getAllRequests(status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String note = body != null ? body.get("resolutionNote") : null;
            return ResponseEntity.ok(planUpgradeRequestService.approveRequest(id, note));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String note = body != null ? body.get("resolutionNote") : null;
            return ResponseEntity.ok(planUpgradeRequestService.rejectRequest(id, note));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
