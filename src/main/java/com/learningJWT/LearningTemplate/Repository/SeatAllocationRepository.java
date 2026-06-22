package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.SeatAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatAllocationRepository extends JpaRepository<SeatAllocation, Long> {

    List<SeatAllocation> findByLibraryIdAndActiveTrue(Long libraryId);

    // Returns first active allocation for student (used for legacy checks)
    Optional<SeatAllocation> findFirstByStudentIdAndActiveTrue(Long studentId);

    // All active allocations for a student
    List<SeatAllocation> findByStudentIdAndActiveTrue(Long studentId);

    // Seats already allocated for a specific slot
    List<SeatAllocation> findBySlotIdAndActiveTrue(Long slotId);

    // Active allocations by student and slot (to prevent double-booking same slot)
    Optional<SeatAllocation> findByStudentIdAndSlotIdAndActiveTrue(Long studentId, Long slotId);

    // Seats allocated in a flexible time range that overlaps with given range
    @Query("SELECT sa FROM SeatAllocation sa WHERE sa.library.id = :libraryId AND sa.active = true " +
           "AND sa.allocationMode = 'FLEXIBLE_HOUR' " +
           "AND sa.seat.id = :seatId " +
           "AND sa.flexStartTime < :endTime AND sa.flexEndTime > :startTime")
    List<SeatAllocation> findOverlappingFlexAllocations(Long libraryId, Long seatId,
                                                         LocalTime startTime, LocalTime endTime);

    // Check if student already has overlapping flex allocation (for student-level overlap check)
    @Query("SELECT sa FROM SeatAllocation sa WHERE sa.library.id = :libraryId AND sa.active = true " +
           "AND sa.allocationMode = 'FLEXIBLE_HOUR' " +
           "AND sa.student.id = :studentId " +
           "AND sa.flexStartTime < :endTime AND sa.flexEndTime > :startTime")
    List<SeatAllocation> findOverlappingFlexAllocationsForStudent(Long libraryId, Long studentId,
                                                                   LocalTime startTime, LocalTime endTime);

    List<SeatAllocation> findByLibraryId(Long libraryId);

    List<SeatAllocation> findByStudentId(Long studentId);

    // All active allocations for a seat
    List<SeatAllocation> findBySeatIdAndActiveTrue(Long seatId);

    // All allocations for a seat (history)
    List<SeatAllocation> findBySeatId(Long seatId);
}
