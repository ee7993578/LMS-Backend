package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Enum.AllocationMode;
import com.learningJWT.LearningTemplate.Enum.FeeStatus;
import com.learningJWT.LearningTemplate.Enum.SeatStatus;
import com.learningJWT.LearningTemplate.Enum.UserRole;
import com.learningJWT.LearningTemplate.Mapper.SeatMapper;
import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Paylod.DTO.SeatAllocationDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.SeatDTO;
import com.learningJWT.LearningTemplate.Paylod.Response.ApiResponse;
import com.learningJWT.LearningTemplate.Repository.*;
import com.learningJWT.LearningTemplate.Services.SeatServices;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatServices {

    private final SeatRepository seatRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final FeeRepository feeRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final SlotRepository slotRepository;

    private User getLoggedInAdmin() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .orElseThrow(() -> new Exception("Logged-in user not found"));
        }
        throw new Exception("No authenticated user found");
    }

    // ---- CRUD ----

    @Override
    public SeatDTO createSeat(SeatDTO dto) throws Exception {
        User admin = getLoggedInAdmin();
        if (admin.getRole() != UserRole.ROLE_LIBRARY_ADMIN) throw new Exception("Not allowed");
        Seat seat = new Seat();
        seat.setSeatName(dto.getSeatName());
        seat.setLocation(dto.getLocation());
        seat.setCreatedAt(LocalDateTime.now());
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setLibrary(admin.getLibrary());
        return SeatMapper.toDto(seatRepository.save(seat));
    }

    @Override
    public SeatDTO updateSeat(Long id, SeatDTO dto) throws Exception {
        User admin = getLoggedInAdmin();
        Seat seat = seatRepository.findById(id).orElseThrow(() -> new Exception("Seat not found"));
        if (!seat.getLibrary().getId().equals(admin.getLibrary().getId())) throw new Exception("Not allowed");
        if (dto.getSeatName() != null) seat.setSeatName(dto.getSeatName());
        if (dto.getLocation() != null) seat.setLocation(dto.getLocation());
        if (dto.getStatus() != null) seat.setStatus(dto.getStatus());
        return SeatMapper.toDto(seatRepository.save(seat));
    }

    @Override
    public void deleteSeat(Long id) throws Exception {
        User admin = getLoggedInAdmin();
        Seat seat = seatRepository.findById(id).orElseThrow(() -> new Exception("Seat not found"));
        if (!seat.getLibrary().getId().equals(admin.getLibrary().getId())) throw new Exception("Not allowed");
        seatRepository.delete(seat);
    }

    @Override
    public List<SeatDTO> getAllSeats() throws Exception {
        User admin = getLoggedInAdmin();
        return seatRepository.findByLibraryId(admin.getLibrary().getId())
                .stream().map(SeatMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public SeatDTO getSeatById(Long id) throws Exception {
        User admin = getLoggedInAdmin();
        Seat seat = seatRepository.findById(id).orElseThrow(() -> new Exception("Seat not found"));
        if (!seat.getLibrary().getId().equals(admin.getLibrary().getId())) throw new Exception("Not allowed");
        return SeatMapper.toDto(seat);
    }

    @Override
    public List<SeatDTO> searchSeat(String search) throws Exception {
        User admin = getLoggedInAdmin();
        return seatRepository.findByLibraryIdAndSeatNameContainingIgnoreCase(admin.getLibrary().getId(), search)
                .stream().map(SeatMapper::toDto).collect(Collectors.toList());
    }

    // ---- Legacy allotment (backward compat) ----

    @Override
    public SeatDTO seatAllotment(Long studentId, Long seatId, Long planId) throws Exception {
        User admin = getLoggedInAdmin();
        Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new Exception("Seat not found"));

        Plan plan = planRepository.findById(planId).orElseThrow(() -> new Exception("Plan not found"));
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new Exception("Student not found"));

        if (!seat.getLibrary().getId().equals(student.getLibrary().getId()) ||
                !seat.getLibrary().getId().equals(admin.getLibrary().getId())) {
            throw new Exception("Library mismatch");
        }

        // Check if student already has an active allocation for this specific seat+plan (prevent exact duplicate)
        Optional<SeatAllocation> exactDuplicate = seatAllocationRepository.findByStudentIdAndActiveTrue(studentId)
                .stream()
                .filter(a -> a.getSeat().getId().equals(seatId) && a.getPlan().getId().equals(planId))
                .findFirst();
        if (exactDuplicate.isPresent()) {
            throw new Exception("Student already has an active allocation for this seat and plan.");
        }

        student.setSeat(seat);
        student.setPlan(plan);
        seat.setStatus(SeatStatus.ALLOCATED);
        if (seat.getStudents() == null) seat.setStudents(new ArrayList<>());
        if (!seat.getStudents().contains(student)) seat.getStudents().add(student);
        seatRepository.save(seat);
        studentRepository.save(student);
        createFeeIfNew(student, plan);

        // Create allocation record
        SeatAllocation alloc = SeatAllocation.builder()
                .student(student).seat(seat).plan(plan)
                .allocationMode(AllocationMode.FLEXIBLE_HOUR)
                .active(true).allocatedAt(LocalDateTime.now())
                .library(admin.getLibrary()).build();
        seatAllocationRepository.save(alloc);

        return SeatMapper.toDto(seat);
    }

    // ---- Fixed Hour Allocation ----

    @Override
    public SeatAllocationDTO allocateFixed(Long studentId, Long seatId, Long planId, Long slotId) throws Exception {
        User admin = getLoggedInAdmin();
        Library library = admin.getLibrary();

        Student student = studentRepository.findById(studentId).orElseThrow(() -> new Exception("Student not found"));
        if (!student.getLibrary().getId().equals(library.getId())) throw new Exception("Student not in your library");

        // Check if student already has this exact slot allocated
        seatAllocationRepository.findByStudentIdAndSlotIdAndActiveTrue(studentId, slotId).ifPresent(a -> {
            throw new RuntimeException("Student already has an active allocation for this slot.");
        });

        Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new Exception("Seat not found"));
        if (!seat.getLibrary().getId().equals(library.getId())) throw new Exception("Seat not in your library");
        if (seat.getStatus() == SeatStatus.UNDER_MAINTENANCE) throw new Exception("Seat is under maintenance");

        Plan plan = planRepository.findById(planId).orElseThrow(() -> new Exception("Plan not found"));
        if (!plan.getLibrary().getId().equals(library.getId())) throw new Exception("Plan not in your library");

        Slot slot = slotRepository.findById(slotId).orElseThrow(() -> new Exception("Slot not found"));
        if (!slot.getLibrary().getId().equals(library.getId())) throw new Exception("Slot not in your library");
        if (!slot.getPlan().getId().equals(planId)) throw new Exception("Slot does not belong to selected plan");

        // Check seat not already allocated for this slot by another student
        List<SeatAllocation> slotAllocs = seatAllocationRepository.findBySlotIdAndActiveTrue(slotId);
        boolean seatTaken = slotAllocs.stream().anyMatch(a -> a.getSeat().getId().equals(seatId));
        if (seatTaken) throw new Exception("Seat is already allocated for this slot");

        // Update seat and student refs
        seat.setStatus(SeatStatus.ALLOCATED);
        student.setSeat(seat);
        student.setPlan(plan);
        seatRepository.save(seat);
        studentRepository.save(student);
        createFeeIfNew(student, plan);

        SeatAllocation alloc = SeatAllocation.builder()
                .student(student).seat(seat).plan(plan).slot(slot)
                .allocationMode(AllocationMode.FIXED_HOUR)
                .active(true).allocatedAt(LocalDateTime.now())
                .library(library).build();

        return toDTO(seatAllocationRepository.save(alloc));
    }

    // ---- Flexible Hour Allocation ----

    @Override
    public SeatAllocationDTO allocateFlexible(Long studentId, Long seatId, Long planId,
                                               LocalTime startTime, LocalTime endTime) throws Exception {
        User admin = getLoggedInAdmin();
        Library library = admin.getLibrary();

        Student student = studentRepository.findById(studentId).orElseThrow(() -> new Exception("Student not found"));
        if (!student.getLibrary().getId().equals(library.getId())) throw new Exception("Student not in your library");

        // Check student doesn't already have overlapping flex allocation
        List<SeatAllocation> studentOverlaps = seatAllocationRepository
                .findOverlappingFlexAllocationsForStudent(library.getId(), studentId, startTime, endTime);
        if (!studentOverlaps.isEmpty()) {
            throw new Exception("Student already has an allocation during this time range: "
                + studentOverlaps.get(0).getFlexStartTime() + " - " + studentOverlaps.get(0).getFlexEndTime());
        }

        Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new Exception("Seat not found"));
        if (!seat.getLibrary().getId().equals(library.getId())) throw new Exception("Seat not in your library");
        if (seat.getStatus() == SeatStatus.UNDER_MAINTENANCE) throw new Exception("Seat is under maintenance");

        Plan plan = planRepository.findById(planId).orElseThrow(() -> new Exception("Plan not found"));

        // Overlap check for this seat
        List<SeatAllocation> seatOverlaps = seatAllocationRepository
                .findOverlappingFlexAllocations(library.getId(), seatId, startTime, endTime);
        if (!seatOverlaps.isEmpty()) throw new Exception("Seat already allocated during this time range");

        seat.setStatus(SeatStatus.ALLOCATED);
        student.setSeat(seat);
        student.setPlan(plan);
        seatRepository.save(seat);
        studentRepository.save(student);
        createFeeIfNew(student, plan);

        SeatAllocation alloc = SeatAllocation.builder()
                .student(student).seat(seat).plan(plan)
                .allocationMode(AllocationMode.FLEXIBLE_HOUR)
                .flexStartTime(startTime).flexEndTime(endTime)
                .active(true).allocatedAt(LocalDateTime.now())
                .library(library).build();

        return toDTO(seatAllocationRepository.save(alloc));
    }

    // ---- Availability Checks ----

    @Override
    public List<SeatDTO> getAvailableSeatsForSlot(Long slotId) throws Exception {
        User admin = getLoggedInAdmin();
        Long libraryId = admin.getLibrary().getId();

        Set<Long> takenSeatIds = seatAllocationRepository.findBySlotIdAndActiveTrue(slotId)
                .stream().map(a -> a.getSeat().getId()).collect(Collectors.toSet());

        return seatRepository.findByLibraryId(libraryId).stream()
                .filter(s -> s.getStatus() != SeatStatus.UNDER_MAINTENANCE)
                .filter(s -> !takenSeatIds.contains(s.getId()))
                .map(SeatMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeatDTO> getAvailableSeatsForFlex(LocalTime startTime, LocalTime endTime) throws Exception {
        User admin = getLoggedInAdmin();
        Long libraryId = admin.getLibrary().getId();

        return seatRepository.findByLibraryId(libraryId).stream()
                .filter(s -> s.getStatus() != SeatStatus.UNDER_MAINTENANCE)
                .filter(s -> {
                    List<SeatAllocation> overlaps = seatAllocationRepository
                            .findOverlappingFlexAllocations(libraryId, s.getId(), startTime, endTime);
                    return overlaps.isEmpty();
                })
                .map(SeatMapper::toDto)
                .collect(Collectors.toList());
    }

    // ---- Allocation Management ----

    @Override
    public List<SeatAllocationDTO> getActiveAllocations() throws Exception {
        User admin = getLoggedInAdmin();
        return seatAllocationRepository.findByLibraryIdAndActiveTrue(admin.getLibrary().getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<SeatAllocationDTO> getAllocationHistory() throws Exception {
        User admin = getLoggedInAdmin();
        return seatAllocationRepository.findByLibraryId(admin.getLibrary().getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ---- Get allocations for a specific seat ----
    public List<SeatAllocationDTO> getSeatAllocations(Long seatId) throws Exception {
        User admin = getLoggedInAdmin();
        Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new Exception("Seat not found"));
        if (!seat.getLibrary().getId().equals(admin.getLibrary().getId())) throw new Exception("Not allowed");
        return seatAllocationRepository.findBySeatIdAndActiveTrue(seatId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ApiResponse SeatDeallocateByStudentId(Long studentId) throws Exception {
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new Exception("Student not found"));

        List<SeatAllocation> activeAllocs = seatAllocationRepository.findByStudentIdAndActiveTrue(studentId);
        for (SeatAllocation a : activeAllocs) {
            a.setActive(false);
            a.setDeallocatedAt(LocalDateTime.now());
            seatAllocationRepository.save(a);
        }

        // Check if seat has other active allocations before setting available
        if (!activeAllocs.isEmpty()) {
            Seat seat = activeAllocs.get(0).getSeat();
            boolean othersActive = seatAllocationRepository.findBySeatIdAndActiveTrue(seat.getId())
                    .stream().anyMatch(x -> !x.getStudent().getId().equals(studentId));
            if (!othersActive) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            }
        }

        student.setSeat(null);
        studentRepository.save(student);

        ApiResponse response = new ApiResponse();
        response.setMessage("Successfully deallocated seat");
        return response;
    }

    @Override
    public ApiResponse deallocateByAllocationId(Long allocationId) throws Exception {
        User admin = getLoggedInAdmin();
        SeatAllocation alloc = seatAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new Exception("Allocation not found"));
        if (!alloc.getLibrary().getId().equals(admin.getLibrary().getId())) throw new Exception("Not allowed");

        alloc.setActive(false);
        alloc.setDeallocatedAt(LocalDateTime.now());
        seatAllocationRepository.save(alloc);

        // Check if student still has other active allocations
        Seat seat = alloc.getSeat();
        Student student = alloc.getStudent();
        boolean studentHasOtherAllocs = seatAllocationRepository.findByStudentIdAndActiveTrue(student.getId())
                .stream().anyMatch(a -> !a.getId().equals(allocationId));
        if (!studentHasOtherAllocs) {
            student.setSeat(null);
            studentRepository.save(student);
        }

        boolean seatStillActive = seatAllocationRepository.findBySeatIdAndActiveTrue(seat.getId()).isEmpty();
        if (seatStillActive) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }

        ApiResponse r = new ApiResponse();
        r.setMessage("Seat deallocated successfully");
        return r;
    }

    // ---- Helpers ----

    private void createFeeIfNew(Student student, Plan plan) {
        if (feeRepository.findByStudentIdAndLibraryId(student.getId(), student.getLibrary().getId()).isEmpty()) {
            Fee fee = new Fee();
            fee.setStudent(student);
            fee.setLibrary(student.getLibrary());
            fee.setFeeStatus(FeeStatus.UNPAID);
            fee.setDueDate(LocalDate.now());
            fee.setMonthId(LocalDate.now().getMonthValue());
            fee.setPayable(plan.getPrice());
            fee.setReceive(0);
            fee.setConcession(0);
            fee.setBalance(plan.getPrice());
            fee.setLateFee(0);
            feeRepository.save(fee);
        }
    }

    private SeatAllocationDTO toDTO(SeatAllocation a) {
        return SeatAllocationDTO.builder()
                .id(a.getId())
                .studentId(a.getStudent().getId())
                .studentName(a.getStudent().getFullName())
                .seatId(a.getSeat().getId())
                .seatName(a.getSeat().getSeatName())
                .seatLocation(a.getSeat().getLocation())
                .planId(a.getPlan().getId())
                .planName(a.getPlan().getName())
                .slotId(a.getSlot() != null ? a.getSlot().getId() : null)
                .slotName(a.getSlot() != null ? a.getSlot().getSlotName() : null)
                .slotStart(a.getSlot() != null ? a.getSlot().getStartTime() : null)
                .slotEnd(a.getSlot() != null ? a.getSlot().getEndTime() : null)
                .allocationMode(a.getAllocationMode())
                .flexStartTime(a.getFlexStartTime())
                .flexEndTime(a.getFlexEndTime())
                .active(a.isActive())
                .allocatedAt(a.getAllocatedAt())
                .deallocatedAt(a.getDeallocatedAt())
                .build();
    }
}
