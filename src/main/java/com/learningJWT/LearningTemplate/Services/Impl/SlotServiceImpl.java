package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Paylod.DTO.SlotDTO;
import com.learningJWT.LearningTemplate.Repository.*;
import com.learningJWT.LearningTemplate.Services.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    private User getLoggedInAdmin() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
        }
        throw new Exception("Not authenticated");
    }

    @Override
    public SlotDTO createSlot(SlotDTO dto) throws Exception {
        User admin = getLoggedInAdmin();
        Library library = admin.getLibrary();

        Plan plan = planRepository.findById(dto.getPlanId())
                .orElseThrow(() -> new Exception("Plan not found"));

        if (!plan.getLibrary().getId().equals(library.getId())) {
            throw new Exception("Plan does not belong to your library");
        }

        validateSlot(dto, library, plan, null);

        int durationHours = computeDurationHours(dto.getStartTime(), dto.getEndTime());

        Slot slot = Slot.builder()
                .slotName(dto.getSlotName())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .durationHours(durationHours)
                .library(library)
                .plan(plan)
                .build();

        return toDTO(slotRepository.save(slot));
    }

    @Override
    public SlotDTO updateSlot(Long slotId, SlotDTO dto) throws Exception {
        User admin = getLoggedInAdmin();
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new Exception("Slot not found"));

        if (!slot.getLibrary().getId().equals(admin.getLibrary().getId())) {
            throw new Exception("Not allowed");
        }

        validateSlot(dto, admin.getLibrary(), slot.getPlan(), slotId);

        slot.setSlotName(dto.getSlotName());
        slot.setStartTime(dto.getStartTime());
        slot.setEndTime(dto.getEndTime());
        slot.setDurationHours(computeDurationHours(dto.getStartTime(), dto.getEndTime()));

        return toDTO(slotRepository.save(slot));
    }

    @Override
    public void deleteSlot(Long slotId) throws Exception {
        User admin = getLoggedInAdmin();
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new Exception("Slot not found"));

        if (!slot.getLibrary().getId().equals(admin.getLibrary().getId())) {
            throw new Exception("Not allowed");
        }

        slotRepository.delete(slot);
    }

    @Override
    public List<SlotDTO> getAllSlots() throws Exception {
        User admin = getLoggedInAdmin();
        return slotRepository.findByLibraryId(admin.getLibrary().getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<SlotDTO> getSlotsByPlan(Long planId) throws Exception {
        User admin = getLoggedInAdmin();
        return slotRepository.findByLibraryIdAndPlanId(admin.getLibrary().getId(), planId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ---- Validation ----

    private void validateSlot(SlotDTO dto, Library library, Plan plan, Long excludeSlotId) throws Exception {
        LocalTime start = dto.getStartTime();
        LocalTime end = dto.getEndTime();

        if (start == null || end == null) throw new Exception("Start and end time are required");

        // Compute slot duration
        int durationHrs = computeDurationHours(start, end);
        long planDuration = plan.getDuration(); // plan duration in hours

        // Slot duration must exactly match plan duration
        if (durationHrs != planDuration) {
            throw new Exception("Slot duration (" + durationHrs + "h) must match plan duration (" + planDuration + "h)");
        }

        // Max slots = 24 / planDuration
        int maxSlots = (int) (24 / planDuration);
        List<Slot> existing = slotRepository.findByLibraryIdAndPlanId(library.getId(), plan.getId());
        long currentCount = existing.stream().filter(s -> !s.getId().equals(excludeSlotId)).count();

        if (currentCount >= maxSlots) {
            throw new Exception("Maximum " + maxSlots + " slot(s) allowed for a " + planDuration + "-hour plan");
        }

        // Check overlap with existing slots for this plan
        for (Slot s : existing) {
            if (s.getId().equals(excludeSlotId)) continue;
            if (timesOverlap(start, end, s.getStartTime(), s.getEndTime())) {
                throw new Exception("Slot overlaps with existing slot: " + s.getSlotName()
                        + " (" + s.getStartTime() + " - " + s.getEndTime() + ")");
            }
        }

        // Continuity check: start of this slot must match end of last slot (if any)
        if (!existing.isEmpty()) {
            List<Slot> sorted = existing.stream()
                    .filter(s -> !s.getId().equals(excludeSlotId))
                    .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                    .collect(Collectors.toList());

            // New slot must either start where last slot ends OR come before first slot
            Slot last = sorted.get(sorted.size() - 1);
            Slot first = sorted.get(0);

            boolean continuesAfterLast = start.equals(last.getEndTime()) || last.getEndTime().equals(LocalTime.MIDNIGHT) && start.equals(LocalTime.MIDNIGHT);
            boolean continuesBefore = end.equals(first.getStartTime());

            if (!continuesAfterLast && !continuesBefore) {
                throw new Exception("Slots must be continuous. Gap detected between " + last.getEndTime() + " and " + start);
            }
        }
    }

    private boolean timesOverlap(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        // Handle midnight-crossing slots: treat midnight (00:00) as 24:00
        // Simple overlap: s1 < e2 AND s2 < e1
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    private int computeDurationHours(LocalTime start, LocalTime end) {
        long minutes;
        if (end.isAfter(start) || end.equals(start)) {
            minutes = Duration.between(start, end).toMinutes();
        } else {
            // crosses midnight
            minutes = Duration.between(start, LocalTime.MIDNIGHT).toMinutes()
                    + Duration.between(LocalTime.MIDNIGHT, end).toMinutes();
            if (minutes == 0) minutes = 24 * 60; // exactly midnight to midnight = 24h
        }
        return (int) (minutes / 60);
    }

    private SlotDTO toDTO(Slot slot) {
        return SlotDTO.builder()
                .id(slot.getId())
                .slotName(slot.getSlotName())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .durationHours(slot.getDurationHours())
                .planId(slot.getPlan().getId())
                .planName(slot.getPlan().getName())
                .libraryId(slot.getLibrary().getId())
                .build();
    }
}
