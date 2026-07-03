package com.learningJWT.LearningTemplate.Enum;

/**
 * How a plan change should affect the student's active cycle:
 * EXTEND  -> stack the new cycle after the existing cycleEnd (or from today if already expired).
 * REPLACE -> end the current cycle today and start the new plan today, with unused value of the
 *            old cycle carried forward as a credit on the new invoice.
 */
public enum SubscriptionChangeMode {
    EXTEND,
    REPLACE
}
