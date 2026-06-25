package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminDashboardDTO {
    private long totalLibraries;
    private long trialLibraries;          // TRIAL + TRIAL_READ_ONLY
    private long activeLibraries;
    private long expiredLibraries;        // EXPIRED_READ_ONLY
    private long inactiveLibraries;
    private long deletedLibraries;
    private long pendingLibraries;

    private long totalStudents;

    private double monthlyRecurringRevenue; // sum of pricePerMonth for ACTIVE subscriptions
    private double totalRevenueCollected;   // sum of pricePerMonth * elapsed months, best-effort estimate
}
