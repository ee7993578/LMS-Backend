package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.AllocationMode;
import com.learningJWT.LearningTemplate.Enum.AttendanceMode;
import com.learningJWT.LearningTemplate.Enum.Status;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryDTO {
    private Long id;
    private String name;
    private String address;
    private String email;
    private String phone;
    private String website;
    private Status status;
    private AllocationMode allocationMode;
    private AttendanceMode attendanceMode;
    private String adminUsername;
    private String adminPassword;
    private String adminFullName;
    private String adminPhone;
    private Long libraryPlanId;
    private LibraryPlanDTO libraryPlan;
}
