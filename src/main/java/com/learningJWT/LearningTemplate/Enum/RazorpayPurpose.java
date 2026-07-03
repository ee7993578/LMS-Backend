package com.learningJWT.LearningTemplate.Enum;

/** What a RazorpayPayment row is paying for. */
public enum RazorpayPurpose {
    /** Paying for a paid LibraryPlan at library-signup time (task: "library create karte waqt"). */
    LIBRARY_SIGNUP,
    /** Paying to switch an existing library to a different (paid) LibraryPlan. */
    LIBRARY_PLAN_UPGRADE
}
