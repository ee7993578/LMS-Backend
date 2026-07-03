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
        student.setAdmissionNumber(dto.getAdmissionNumber());
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
        dto.setAdmissionNumber(student.getAdmissionNumber());
        dto.setSeatId(student.getSeat() != null ? student.getSeat().getId() : null);
        dto.setPlanId(student.getPlan() != null ? student.getPlan().getId() : null);
        dto.setPlan(student.getPlan() != null ? PlanMapper.toDto(student.getPlan()) : null);
        dto.setDateOfJoin(student.getDateOfJoin());

        // Fallback expiry (only used when no active StudentSubscription row exists yet —
        // normally StudentSubscriptionService.enrichStudentDTO overrides this with the
        // real cycle end). Month-based plans expire on the same date N month(s) later;
        // day-based/custom plans keep the old dateOfJoin + subscriptionDays behaviour.
        if (student.getDateOfJoin() != null && student.getPlan() != null) {
            com.learningJWT.LearningTemplate.Enum.PlanDurationType type = student.getPlan().getDurationType();
            Integer months = student.getPlan().getDurationMonths();
            if (type == com.learningJWT.LearningTemplate.Enum.PlanDurationType.MONTHS && months != null && months > 0) {
                dto.setSubscriptionExpiryDate(student.getDateOfJoin().plusMonths(months));
            } else {
                Integer subDays = student.getPlan().getSubscriptionDays();
                if (subDays == null && student.getPlan().getDuration() != null) {
                    subDays = student.getPlan().getDuration().intValue();
                }
                if (subDays != null && subDays > 0) {
                    dto.setSubscriptionExpiryDate(student.getDateOfJoin().plusDays(subDays));
                }
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
