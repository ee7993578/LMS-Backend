package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Model.Seat;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Paylod.DTO.SeatDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentDTO;

import java.util.List;
import java.util.stream.Collectors;

public class SeatMapper {

    public static SeatDTO toDTO(Seat seat) {
        if (seat == null) return null;
        SeatDTO dto = SeatDTO.builder()
                .id(seat.getId())
                .seatName(seat.getSeatName())
                .location(seat.getLocation())
                .status(seat.getStatus())
                .build();

        if (seat.getLibrary() != null) {
            dto.setLibraryId(seat.getLibrary().getId());
        }

        // Map students without their seat (avoid circular reference)
        if (seat.getStudents() != null && !seat.getStudents().isEmpty()) {
            List<StudentDTO> studentDTOs = seat.getStudents().stream()
                    .map(SeatMapper::toStudentDTOFlat)
                    .collect(Collectors.toList());
            dto.setStudents(studentDTOs);
            // Also set current student if only one occupant
            if (studentDTOs.size() == 1) {
                dto.setStudentId(studentDTOs.get(0).getId());
            }
        }

        return dto;
    }

    /** Alias for backward compatibility */
    public static SeatDTO toDto(Seat seat) {
        return toDTO(seat);
    }

    /** Flat student mapping (no nested seat) to avoid circular reference */
    private static StudentDTO toStudentDTOFlat(Student student) {
        if (student == null) return null;
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setFullName(student.getFullName());
        dto.setEmail(student.getEmail());
        dto.setPhone(student.getPhone());
        dto.setPlanId(student.getPlan() != null ? student.getPlan().getId() : null);
        dto.setPlan(student.getPlan() != null ? PlanMapper.toDto(student.getPlan()) : null);
        if (student.getUser() != null) {
            dto.setUsername(student.getUser().getUsername());
        }
        return dto;
    }

    public static Seat toEntity(SeatDTO dto) {
        if (dto == null) return null;
        return Seat.builder()
                .id(dto.getId())
                .seatName(dto.getSeatName())
                .location(dto.getLocation())
                .status(dto.getStatus())
                .build();
    }
}
