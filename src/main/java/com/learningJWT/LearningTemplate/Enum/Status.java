package com.learningJWT.LearningTemplate.Enum;

/**
 * Unified status enum used by BOTH Library (lifecycle status) and Subscription
 * (billing status). Not every value applies to both entities — see comments.
 *
 * ===== LIBRARY LIFECYCLE STATUS (Library.status) =====
 * PENDING            -> library created by SuperAdmin but trial/subscription not started yet
 * TRIAL              -> first 7 days after registration. Full access.
 * TRIAL_READ_ONLY    -> 7 days after trial expiry. Read-only, no writes.
 * ACTIVE             -> paid subscription is active. Full access.
 * EXPIRED_READ_ONLY  -> 7 days after subscription expiry. Read-only, no writes.
 * INACTIVE           -> login blocked, no API access at all.
 * DELETED            -> library permanently removed from active system (soft-deleted, never hard-deleted
 *                       to preserve student data integrity; excluded from all active queries).
 *
 * ===== SUBSCRIPTION BILLING STATUS (Subscription.status) =====
 * ACTIVE   -> subscription currently paid & valid
 * EXPIRED  -> billingEnd has passed, library is now in its grace/read-only window
 * GRACE    -> legacy alias, retained for backward compatibility with existing data/queries
 * INACTIVE -> grace window over, subscription fully lapsed
 *
 * EXCEEDED is retained for backward compatibility (legacy student-buffer-exceeded marker);
 * the new grace-period system tracks this on Library/Subscription directly instead.
 */
public enum Status {
    PENDING,
    TRIAL,
    TRIAL_READ_ONLY,
    ACTIVE,
    EXPIRED_READ_ONLY,
    INACTIVE,
    DELETED,

    // Legacy / subscription-internal values retained for backward compatibility
    GRACE,
    EXPIRED,
    EXCEEDED
}
