package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Paylod.DTO.OnboardingStatusDTO;

/**
 * Central, reusable source of truth for "how far along is this library's first-time setup".
 * Both the dashboard checklist and the one-time welcome modal read from this. Adding a future
 * onboarding step means adding one entry to the STEP DEFINITIONS list in the impl — nothing
 * else needs to change.
 */
public interface OnboardingStatusService {

    OnboardingStatusDTO getStatus(Library library);

    /** Marks the one-time welcome modal as seen so it never shows again for this library. */
    void markWelcomeSeen(Library library);
}
