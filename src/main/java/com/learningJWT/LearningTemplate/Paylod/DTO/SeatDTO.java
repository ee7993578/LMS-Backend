package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDTO {

    private Long id;
    private String seatName;
    private String location;
    private SeatStatus status;
    private LibraryDTO library;
    private String email;
    private StudentDTO student;
    private Long libraryId;
    private List<StudentDTO> students;
    private Long studentId;
}
