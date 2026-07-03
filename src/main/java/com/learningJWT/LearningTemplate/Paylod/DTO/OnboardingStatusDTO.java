package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStatusDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
        private String key;            // stable identifier, e.g. "PLANS"
        private String title;          // "Create Student Plans"
        private String description;    // why this step matters, 1-2 lines
        private String estimatedTime;  // "2 Minutes"
        private String actionLabel;    // "Create Plan"
        private String actionRoute;    // frontend route to send the admin to
        private boolean completed;
    }

    private boolean welcomeShown;
    private List<Step> steps;
    private int completedCount;
    private int totalCount;
    private int percentage;
    private boolean allCompleted;
    private Step recommendedNextStep; // null when allCompleted
}
