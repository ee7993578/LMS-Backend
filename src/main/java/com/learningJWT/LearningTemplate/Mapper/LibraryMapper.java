package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Enum.AllocationMode;
import com.learningJWT.LearningTemplate.Enum.AttendanceMode;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryPlanDTO;
import org.springframework.stereotype.Component;

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
                    .build();
            dto.setLibraryPlan(planDTO);
        }

        if (library.getAdmin() != null) {
            dto.setAdminUsername(library.getAdmin().getUsername());
            dto.setAdminFullName(library.getAdmin().getFullName());
            dto.setAdminPhone(library.getAdmin().getPhone());
        }
        return dto;
    }
}
