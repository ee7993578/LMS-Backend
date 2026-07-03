package com.learningJWT.LearningTemplate.Enum;

/**
 * Lifecycle status of a single StudentSubscription cycle row.
 * EXPIRING_SOON is NOT stored in the DB — it is computed at read time by
 * StudentSubscriptionService.computeDisplayStatus() from an ACTIVE row whose
 * cycleEnd is within the warning window. GRACE is reserved for future use
 * (e.g. a library-level grace period applied on top of an expired cycle).
 */
public enum StudentSubscriptionStatus {
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    GRACE,
    RENEWED,
    UPGRADED,
    CANCELLED
}
