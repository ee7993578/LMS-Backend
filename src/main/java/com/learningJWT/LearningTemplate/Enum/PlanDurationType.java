package com.learningJWT.LearningTemplate.Enum;

public enum PlanDurationType {
    // Calendar-month based plan (1/2/3/4/6/12 months). Expiry always falls on the
    // same date next month(s) — e.g. joined 5 Jan on a 1-month plan expires 5 Feb,
    // joined 31 Jan expires last day of Feb (handled by java.time.LocalDate#plusMonths).
    MONTHS,
    // Fixed number-of-days plan (the older/custom behaviour, unchanged).
    DAYS
}
