package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.AllocationMode;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAllocationDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long seatId;
    private String seatName;
    private String seatLocation;
    private Long planId;
    private String planName;
    private Long slotId;
    private String slotName;
    private LocalTime slotStart;
    private LocalTime slotEnd;
    private AllocationMode allocationMode;
    private LocalTime flexStartTime;
    private LocalTime flexEndTime;
    private boolean active;
    private LocalDateTime allocatedAt;
    private LocalDateTime deallocatedAt;
}
