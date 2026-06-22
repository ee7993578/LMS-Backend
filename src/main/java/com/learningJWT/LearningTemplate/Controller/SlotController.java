package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Paylod.DTO.SlotDTO;
import com.learningJWT.LearningTemplate.Services.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/slot")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PostMapping
    public ResponseEntity<?> createSlot(@RequestBody SlotDTO dto) {
        try {
            return ResponseEntity.ok(slotService.createSlot(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSlot(@PathVariable Long id, @RequestBody SlotDTO dto) {
        try {
            return ResponseEntity.ok(slotService.updateSlot(id, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSlot(@PathVariable Long id) {
        try {
            slotService.deleteSlot(id);
            return ResponseEntity.ok("Slot deleted");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping
    public ResponseEntity<List<SlotDTO>> getAllSlots() throws Exception {
        return ResponseEntity.ok(slotService.getAllSlots());
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/plan/{planId}")
    public ResponseEntity<List<SlotDTO>> getSlotsByPlan(@PathVariable Long planId) throws Exception {
        return ResponseEntity.ok(slotService.getSlotsByPlan(planId));
    }
}
