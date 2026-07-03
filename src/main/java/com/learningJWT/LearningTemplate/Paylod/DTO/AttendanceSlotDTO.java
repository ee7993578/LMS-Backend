package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSlotDTO {

    private Integer slotNumber;
    private LocalDateTime punchIn;
    private LocalDateTime punchOut;   // null while this slot is still open
    private Long durationMinutes;     // null while still open
    private Boolean autoClosed;
}
