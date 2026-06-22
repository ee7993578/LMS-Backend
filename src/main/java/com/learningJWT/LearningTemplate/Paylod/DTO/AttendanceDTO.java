package com.learningJWT.LearningTemplate.Paylod.DTO;


import com.learningJWT.LearningTemplate.Enum.AttendanceStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDTO {

    private Long id;

    private Long studentId;
    private String studentName;

    private Long libraryId;
    private String libraryName;

    private LocalDate attendanceDate;

    private AttendanceStatus  attendanceStatus;
    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;

    private Long totalStudyMinutes;
    private Long totalBreakMinutes;

}
