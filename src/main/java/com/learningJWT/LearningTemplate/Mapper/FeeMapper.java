package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Model.Fee;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentDTO;

public class FeeMapper {

    public static FeeDTO toDTO(Fee fee) {
        if (fee == null) return null;
        FeeDTO dto = FeeDTO.builder()
                .feeId(fee.getFeeId())
                .monthId(fee.getMonthId())
                .balance(fee.getBalance())
                .payable(fee.getPayable())
                .Receive(fee.getReceive())
                .concession(fee.getConcession())
                .lateFee(fee.getLateFee())
                .dueDate(fee.getDueDate())
                .paymentDate(fee.getPaymentDate())
                .feeStatus(fee.getFeeStatus())
                .build();

        if (fee.getLibrary() != null) {
            dto.setLibraryId(fee.getLibrary().getId());
        }
        if (fee.getStudent() != null) {
            dto.setStudentId(fee.getStudent().getId());
            // Embed a lightweight student object so frontend can show student name
            StudentDTO studentDTO = new StudentDTO();
            studentDTO.setId(fee.getStudent().getId());
            studentDTO.setFullName(fee.getStudent().getFullName());
            studentDTO.setEmail(fee.getStudent().getEmail());
            studentDTO.setPhone(fee.getStudent().getPhone());
            dto.setStudent(studentDTO);
        }
        return dto;
    }

    public static Fee toEntity(FeeDTO feeDTO) {
        if (feeDTO == null) return null;
        return Fee.builder()
                .feeId(feeDTO.getFeeId())
                .monthId(feeDTO.getMonthId())
                .balance(feeDTO.getBalance())
                .payable(feeDTO.getPayable())
                .Receive(feeDTO.getReceive())
                .concession(feeDTO.getConcession())
                .lateFee(feeDTO.getLateFee())
                .dueDate(feeDTO.getDueDate())
                .paymentDate(feeDTO.getPaymentDate())
                .feeStatus(feeDTO.getFeeStatus())
                .build();
    }
}
