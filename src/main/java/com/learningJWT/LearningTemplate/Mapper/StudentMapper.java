package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Paylod.DTO.SeatDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class StudentMapper {

    public static Student toEntity(StudentDTO dto) {
        if (dto == null) return null;
        Student student = new Student();
        student.setId(dto.getId());
        student.setFullName(dto.getFullName());
        student.setEmail(dto.getEmail());
        student.setPhone(dto.getPhone());
        student.setDateOfJoin(dto.getDateOfJoin() != null ? dto.getDateOfJoin() : LocalDate.now());
        return student;
    }

    public static StudentDTO toDTO(Student student) {
        if (student == null) return null;
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setFullName(student.getFullName());
        dto.setEmail(student.getEmail());
        dto.setPhone(student.getPhone());
        dto.setSeatId(student.getSeat() != null ? student.getSeat().getId() : null);
        dto.setPlanId(student.getPlan() != null ? student.getPlan().getId() : null);
        dto.setPlan(student.getPlan() != null ? PlanMapper.toDto(student.getPlan()) : null);
        dto.setDateOfJoin(student.getDateOfJoin());

        // Compute subscription expiry: dateOfJoin + subscriptionDays
        if (student.getDateOfJoin() != null && student.getPlan() != null) {
            Integer subDays = student.getPlan().getSubscriptionDays();
            if (subDays == null && student.getPlan().getDuration() != null) {
                subDays = student.getPlan().getDuration().intValue();
            }
            if (subDays != null && subDays > 0) {
                dto.setSubscriptionExpiryDate(student.getDateOfJoin().plusDays(subDays));
            }
        }

        if (student.getUser() != null) {
            dto.setUsername(student.getUser().getUsername());
        }
        // Set flat seat info (no students list inside to avoid circular ref)
        if (student.getSeat() != null) {
            SeatDTO seatDTO = new SeatDTO();
            seatDTO.setId(student.getSeat().getId());
            seatDTO.setSeatName(student.getSeat().getSeatName());
            seatDTO.setLocation(student.getSeat().getLocation());
            seatDTO.setStatus(student.getSeat().getStatus());
            dto.setSeat(seatDTO);
        }
        return dto;
    }
}
