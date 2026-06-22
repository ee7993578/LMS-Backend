package com.learningJWT.LearningTemplate.Services;


import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.SubscriptionDTO;

import java.util.List;

public interface SubscriptionService {

    // 1️⃣ Create or assign subscription to a library
    SubscriptionDTO createSubscription(Long libraryId, Long planId) throws Exception;

    // 2️⃣ Upgrade or change subscription plan
    SubscriptionDTO upgradePlan(Long libraryId, Long newPlanId) throws Exception;

    // 3️⃣ Renew subscription (extend billingEnd)
    SubscriptionDTO renewSubscription(Long libraryId) throws Exception;

    // 4️⃣ Check if a library subscription is active or expired
    boolean isSubscriptionActive(Long libraryId) throws Exception;

    // 5️⃣ Handle buffer overflow (when student > limit + buffer)
    void handleBufferExceeded(Long libraryId)  throws Exception;

    // 6️⃣ Delete buffer students after expiry days (run via @Scheduled)
    void deleteExpiredBufferStudents()  throws Exception;

    // 7️⃣ Mark expired subscriptions as INACTIVE (daily check)
    void deactivateExpiredSubscriptions()  throws Exception;

    // 8️⃣ Permanently delete library data after 30 days of inactivity
    void deleteLibrariesAfterGracePeriod()  throws Exception;

    // 9️⃣ Get all subscriptions
    List<LibraryDTO> getAllLibraryBySubscriptionStatus(Status subscriptionStatus) throws Exception;

    // 🔟 Get subscription details for specific library
    SubscriptionDTO getSubscriptionByLibraryId(Long libraryId)  throws Exception;
}
