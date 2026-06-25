package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryUsageDTO {
    private Long libraryId;
    private String libraryName;
    private String planName;
    private Integer planLimit;
    private Integer graceLimit;       // planLimit + bufferStudent
    private long currentStudentCount;
    private boolean inGracePeriod;
    private Long graceDaysRemaining;  // null if not in grace
    private boolean graceExceeded;    // true once past planLimit + bufferStudent

    // ===== Library lifecycle status (for banners) =====
    private Status status;
    private Long daysRemainingInCurrentPhase; // null when not applicable (ACTIVE/DELETED/PENDING)
}
