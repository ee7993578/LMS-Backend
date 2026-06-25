package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Enum.AllocationMode;
import com.learningJWT.LearningTemplate.Enum.AttendanceMode;
import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryPlanDTO;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class LibraryMapper {

    public static Library toEntity(LibraryDTO dto) {
        Library library = new Library();
        library.setName(dto.getName());
        library.setAddress(dto.getAddress());
        library.setEmail(dto.getEmail());
        library.setPhone(dto.getPhone());
        library.setWebsite(dto.getWebsite());
        library.setStatus(dto.getStatus());
        library.setAllocationMode(dto.getAllocationMode() != null ? dto.getAllocationMode() : AllocationMode.FLEXIBLE_HOUR);
        library.setAttendanceMode(dto.getAttendanceMode() != null ? dto.getAttendanceMode() : AttendanceMode.BOTH);
        return library;
    }

    public static LibraryDTO toDTO(Library library) {
        LibraryDTO dto = new LibraryDTO();
        dto.setId(library.getId());
        dto.setName(library.getName());
        dto.setAddress(library.getAddress());
        dto.setEmail(library.getEmail());
        dto.setPhone(library.getPhone());
        dto.setWebsite(library.getWebsite());
        dto.setStatus(library.getStatus());
        dto.setAllocationMode(library.getAllocationMode() != null ? library.getAllocationMode() : AllocationMode.FLEXIBLE_HOUR);
        dto.setAttendanceMode(library.getAttendanceMode() != null ? library.getAttendanceMode() : AttendanceMode.BOTH);

        dto.setTrialStartDate(library.getTrialStartDate());
        dto.setTrialEndDate(library.getTrialEndDate());
        dto.setStatusChangedAt(library.getStatusChangedAt());
        dto.setDaysRemainingInCurrentPhase(computeDaysRemaining(library));

        if (library.getLibraryPlan() != null) {
            dto.setLibraryPlanId(library.getLibraryPlan().getPlanId());
            LibraryPlanDTO planDTO = LibraryPlanDTO.builder()
                    .planId(library.getLibraryPlan().getPlanId())
                    .planName(library.getLibraryPlan().getPlanName())
                    .planPrice(library.getLibraryPlan().getPlanPrice())
                    .noOfStudent(library.getLibraryPlan().getNoOfStudent())
                    .bufferStudent(library.getLibraryPlan().getBufferStudent())
                    .planOrder(library.getLibraryPlan().getPlanOrder())
                    .noOfDays(library.getLibraryPlan().getNoOfDays())
                    .description(library.getLibraryPlan().getDescription())
                    .isActive(library.getLibraryPlan().getIsActive())
                    .gracePeriodDays(library.getLibraryPlan().getGracePeriodDays())
                    .build();
            dto.setLibraryPlan(planDTO);
            dto.setPlanLimit(library.getLibraryPlan().getNoOfStudent());
            dto.setGraceLimit(library.getLibraryPlan().getBufferStudent());
        }

        if (library.getAdmin() != null) {
            dto.setAdminUsername(library.getAdmin().getUsername());
            dto.setAdminFullName(library.getAdmin().getFullName());
            dto.setAdminPhone(library.getAdmin().getPhone());
        }
        return dto;
    }

    /** Days left in the library's CURRENT phase (trial, trial-read-only, expired-read-only,
     *  or inactive-before-deletion). Returns null for ACTIVE / DELETED / PENDING since there's
     *  no countdown to show for those. Never negative — floors at 0. */
    private static Long computeDaysRemaining(Library library) {
        if (library.getStatus() == null) return null;
        LocalDateTime now = LocalDateTime.now();
        switch (library.getStatus()) {
            case TRIAL:
                if (library.getTrialEndDate() == null) return null;
                return Math.max(0, Duration.between(now, library.getTrialEndDate()).toDays());
            case TRIAL_READ_ONLY:
                if (library.getStatusChangedAt() == null) return null;
                return Math.max(0, 7 - Duration.between(library.getStatusChangedAt(), now).toDays());
            case EXPIRED_READ_ONLY:
                if (library.getStatusChangedAt() == null) return null;
                return Math.max(0, 7 - Duration.between(library.getStatusChangedAt(), now).toDays());
            case INACTIVE:
                if (library.getStatusChangedAt() == null) return null;
                // Ambiguous which inactive-window applies (30d post-trial vs 90d post-subscription)
                // without knowing prior phase; SuperAdmin services compute the precise figure where needed.
                return Math.max(0, 30 - Duration.between(library.getStatusChangedAt(), now).toDays());
            default:
                return null;
        }
    }
}
