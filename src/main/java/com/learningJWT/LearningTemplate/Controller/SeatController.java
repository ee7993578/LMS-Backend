package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Paylod.DTO.SeatAllocationDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.SeatDTO;
import com.learningJWT.LearningTemplate.Paylod.Response.ApiResponse;
import com.learningJWT.LearningTemplate.Services.SeatServices;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/seat")
@RequiredArgsConstructor
public class SeatController {

    private final SeatServices seatService;

    // ---- CRUD ----

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<SeatDTO> createSeat(@RequestBody SeatDTO dto) throws Exception {
        return ResponseEntity.ok(seatService.createSeat(dto));
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping("/update/{id}")
    public ResponseEntity<SeatDTO> updateSeat(@PathVariable Long id, @RequestBody SeatDTO dto) throws Exception {
        return ResponseEntity.ok(seatService.updateSeat(id, dto));
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteSeat(@PathVariable Long id) throws Exception {
        seatService.deleteSeat(id);
        return ResponseEntity.ok("Seat deleted successfully");
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<SeatDTO>> getAllSeats() throws Exception {
        return ResponseEntity.ok(seatService.getAllSeats());
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<SeatDTO> getSeatById(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<SeatDTO>> searchSeats(@RequestParam String keyword) throws Exception {
        return ResponseEntity.ok(seatService.searchSeat(keyword));
    }

    // ---- Legacy allotment ----

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping("/allotment")
    public ResponseEntity<?> seatAllotment(@RequestParam Long studentId,
                                            @RequestParam Long seatId,
                                            @RequestParam Long planId) {
        try {
            return ResponseEntity.ok(seatService.seatAllotment(studentId, seatId, planId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping("/deallocate/{studentId}")
    public ResponseEntity<?> deallocateSeat(@PathVariable Long studentId) {
        try {
            return ResponseEntity.ok(seatService.SeatDeallocateByStudentId(studentId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---- New: Fixed-hour allocation ----

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PostMapping("/allocate/fixed")
    public ResponseEntity<?> allocateFixed(@RequestParam Long studentId,
                                            @RequestParam Long seatId,
                                            @RequestParam Long planId,
                                            @RequestParam Long slotId) {
        try {
            return ResponseEntity.ok(seatService.allocateFixed(studentId, seatId, planId, slotId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---- New: Flexible-hour allocation ----

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PostMapping("/allocate/flexible")
    public ResponseEntity<?> allocateFlexible(@RequestParam Long studentId,
                                               @RequestParam Long seatId,
                                               @RequestParam Long planId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        try {
            return ResponseEntity.ok(seatService.allocateFlexible(studentId, seatId, planId, startTime, endTime));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---- New: Seat availability ----

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/available/slot/{slotId}")
    public ResponseEntity<List<SeatDTO>> availableForSlot(@PathVariable Long slotId) throws Exception {
        return ResponseEntity.ok(seatService.getAvailableSeatsForSlot(slotId));
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/available/flex")
    public ResponseEntity<List<SeatDTO>> availableForFlex(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) throws Exception {
        return ResponseEntity.ok(seatService.getAvailableSeatsForFlex(startTime, endTime));
    }

    // ---- New: Allocation management ----

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/allocations/active")
    public ResponseEntity<List<SeatAllocationDTO>> getActiveAllocations() throws Exception {
        return ResponseEntity.ok(seatService.getActiveAllocations());
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/allocations/history")
    public ResponseEntity<List<SeatAllocationDTO>> getAllocationHistory() throws Exception {
        return ResponseEntity.ok(seatService.getAllocationHistory());
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping("/allocations/{allocationId}/deallocate")
    public ResponseEntity<?> deallocateByAllocationId(@PathVariable Long allocationId) {
        try {
            return ResponseEntity.ok(seatService.deallocateByAllocationId(allocationId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---- New: Get active allocations for a specific seat (for seat detail view) ----

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/{seatId}/allocations")
    public ResponseEntity<?> getSeatAllocations(@PathVariable Long seatId) {
        try {
            return ResponseEntity.ok(seatService.getSeatAllocations(seatId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
