package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Enum.PlanUsageStatus;
import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import com.learningJWT.LearningTemplate.Model.Subscription;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Repository.LibraryPlanRepository;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Services.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Single source of truth for library/subscription lifecycle transitions and plan-usage
 * (grace period) calculations. Both the SuperAdmin module and the scheduled lifecycle
 * jobs go through here so the rules are defined exactly once.
 */
@Service
@RequiredArgsConstructor
public class LibraryLifecycleService {

    public static final int TRIAL_DAYS = 7;
    public static final int TRIAL_READ_ONLY_DAYS = 7;
    public static final int TRIAL_INACTIVE_TO_DELETE_DAYS = 30;

    public static final int SUBSCRIPTION_EXPIRED_READ_ONLY_DAYS = 7;
    public static final int SUBSCRIPTION_INACTIVE_TO_DELETE_DAYS = 90;

    public static final int DEFAULT_GRACE_PERIOD_DAYS = 3;

    private final LibraryRepository libraryRepository;
    private final StudentRepository studentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LibraryPlanRepository libraryPlanRepository;

    // ===================== REGISTRATION =====================

    /**
     * Called once when a library registers. Per product decision: there is no separate
     * TRIAL phase anymore — every new library is assigned the lowest-`planOrder` plan
     * (the "free"/starter plan) automatically and started directly as ACTIVE, with a full
     * billing cycle based on that plan's noOfDays. The library admin can later request an
     * upgrade to any other plan from their settings page (see PlanUpgradeRequest flow);
     * SuperAdmin approves/rejects that request.
     */
    public LibraryPlan assignDefaultPlanAndActivate(Library library) {
        LibraryPlan defaultPlan = libraryPlanRepository.findByIsActiveTrue().stream()
                .min(java.util.Comparator.comparing(p -> p.getPlanOrder() == null ? Integer.MAX_VALUE : p.getPlanOrder()))
                .orElse(null);

        library.setLibraryPlan(defaultPlan);
        transitionTo(library, Status.ACTIVE);
        return defaultPlan;
    }

    // ===================== STATUS TRANSITIONS =====================

    /** Moves a library to a new status, stamping statusChangedAt so the next
     *  scheduled transition (read-only window, inactive countdown, etc.) is timed correctly. */
    public void transitionTo(Library library, Status newStatus) {
        library.setStatus(newStatus);
        library.setStatusChangedAt(LocalDateTime.now());
        library.setUpdatedAt(LocalDateTime.now());
    }

    /** SuperAdmin-triggered: activate a library (e.g. after manual payment confirmation
     *  or to lift a suspension). Does not touch trial dates. */
    public void activate(Library library) {
        transitionTo(library, Status.ACTIVE);
    }

    /** SuperAdmin-triggered: suspend a library immediately (login blocked). */
    public void suspend(Library library) {
        transitionTo(library, Status.INACTIVE);
    }

    /** SuperAdmin-triggered: soft-delete. Library and its students/data are preserved in the
     *  database; the library is simply excluded from active operation. Reversible by SuperAdmin. */
    public void softDelete(Library library) {
        transitionTo(library, Status.DELETED);
    }

    // ===================== PLAN USAGE / GRACE PERIOD =====================

    /** How many ACTIVE students a library currently has. */
    public long currentStudentCount(Long libraryId) {
        return studentRepository.countByLibraryIdAndActiveTrue(libraryId);
    }

    public PlanUsageStatus evaluatePlanUsage(Library library) {
        LibraryPlan plan = library.getLibraryPlan();
        if (plan == null || plan.getNoOfStudent() == null) return PlanUsageStatus.WITHIN_LIMIT;

        long count = currentStudentCount(library.getId());
        int limit = plan.getNoOfStudent();
        int graceLimit = limit + (plan.getBufferStudent() != null ? plan.getBufferStudent() : 0);

        if (count <= limit) return PlanUsageStatus.WITHIN_LIMIT;
        if (count <= graceLimit) return PlanUsageStatus.IN_GRACE;
        return PlanUsageStatus.GRACE_EXCEEDED;
    }

    /** Whether a NEW student may be registered/imported right now, given current count + plan.
     *  Existing students are never affected by this check — it only gates creation of new ones. */
    public boolean canRegisterNewStudent(Library library) {
        LibraryPlan plan = library.getLibraryPlan();
        if (plan == null || plan.getNoOfStudent() == null) return true; // no plan limit configured

        long count = currentStudentCount(library.getId());
        int limit = plan.getNoOfStudent();
        int graceLimit = limit + (plan.getBufferStudent() != null ? plan.getBufferStudent() : 0);

        if (count < limit) return true; // comfortably within limit

        if (count < graceLimit) {
            // Entering/continuing grace period — start the clock the first time this happens.
            ensureGraceStarted(library);
            return !isGraceExpired(library, plan);
        }

        return false; // at or beyond limit+buffer — blocked regardless of grace timer
    }

    private void ensureGraceStarted(Library library) {
        if (library.getGracePeriodStartedAt() == null) {
            library.setGracePeriodStartedAt(LocalDateTime.now());
            libraryRepository.save(library);
        }
    }

    private boolean isGraceExpired(Library library, LibraryPlan plan) {
        if (library.getGracePeriodStartedAt() == null) return false;
        int graceDays = plan.getGracePeriodDays() != null ? plan.getGracePeriodDays() : DEFAULT_GRACE_PERIOD_DAYS;
        return LocalDateTime.now().isAfter(library.getGracePeriodStartedAt().plusDays(graceDays));
    }

    /** Clears the grace timer once a library is back within its plan limit (e.g. after upgrade). */
    public void clearGraceIfBackWithinLimit(Library library) {
        LibraryPlan plan = library.getLibraryPlan();
        if (plan == null || plan.getNoOfStudent() == null) {
            library.setGracePeriodStartedAt(null);
            return;
        }
        long count = currentStudentCount(library.getId());
        if (count <= plan.getNoOfStudent()) {
            library.setGracePeriodStartedAt(null);
        }
    }

    /** Days left before the grace window closes (null if not in grace). */
    public Long graceDaysRemaining(Library library) {
        if (library.getGracePeriodStartedAt() == null) return null;
        LibraryPlan plan = library.getLibraryPlan();
        int graceDays = (plan != null && plan.getGracePeriodDays() != null) ? plan.getGracePeriodDays() : DEFAULT_GRACE_PERIOD_DAYS;
        long elapsed = java.time.Duration.between(library.getGracePeriodStartedAt(), LocalDateTime.now()).toDays();
        return Math.max(0, graceDays - elapsed);
    }
}
