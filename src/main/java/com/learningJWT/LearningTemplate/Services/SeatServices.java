package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.SeatAllocationDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.SeatDTO;
import com.learningJWT.LearningTemplate.Paylod.Response.ApiResponse;

import java.time.LocalTime;
import java.util.List;

public interface SeatServices {
    // Existing seat CRUD
    SeatDTO createSeat(SeatDTO dto) throws Exception;
    SeatDTO updateSeat(Long id, SeatDTO dto) throws Exception;
    void deleteSeat(Long id) throws Exception;
    List<SeatDTO> getAllSeats() throws Exception;
    SeatDTO getSeatById(Long id) throws Exception;
    List<SeatDTO> searchSeat(String search) throws Exception;

    // Legacy allotment (kept for backward compatibility)
    SeatDTO seatAllotment(Long studentId, Long seatId, Long planId) throws Exception;
    ApiResponse SeatDeallocateByStudentId(Long studentId) throws Exception;

    // New: Fixed-hour allocation (with slot)
    SeatAllocationDTO allocateFixed(Long studentId, Long seatId, Long planId, Long slotId) throws Exception;

    // New: Flexible-hour allocation (with time range)
    SeatAllocationDTO allocateFlexible(Long studentId, Long seatId, Long planId,
                                       LocalTime startTime, LocalTime endTime) throws Exception;

    // New: check available seats for a slot
    List<SeatDTO> getAvailableSeatsForSlot(Long slotId) throws Exception;

    // New: check available seats for a flex time range
    List<SeatDTO> getAvailableSeatsForFlex(LocalTime startTime, LocalTime endTime) throws Exception;

    // New: active allocations
    List<SeatAllocationDTO> getActiveAllocations() throws Exception;
    List<SeatAllocationDTO> getAllocationHistory() throws Exception;

    // New: deallocate by allocation id
    ApiResponse deallocateByAllocationId(Long allocationId) throws Exception;

    // New: get all active allocations for a specific seat
    List<SeatAllocationDTO> getSeatAllocations(Long seatId) throws Exception;
}
