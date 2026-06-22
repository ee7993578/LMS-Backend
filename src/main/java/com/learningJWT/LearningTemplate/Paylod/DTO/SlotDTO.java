package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.*;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotDTO {
    private Long id;
    private String slotName;
    private LocalTime startTime;
    private LocalTime endTime;
    private int durationHours;
    private Long planId;
    private String planName;
    private Long libraryId;
}
