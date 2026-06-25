package com.learningJWT.LearningTemplate.Enum;

/**
 * Tracks whether a library is within its plan's student limit, using grace
 * students (over limit but under limit+grace), or has exceeded grace too
 * (over limit+grace — new student creation is blocked, existing students
 * are NEVER touched).
 */
public enum PlanUsageStatus {
    WITHIN_LIMIT,
    IN_GRACE,
    GRACE_EXCEEDED
}
