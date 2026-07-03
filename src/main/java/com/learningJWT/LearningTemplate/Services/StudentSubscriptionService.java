package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Enum.StudentSubscriptionStatus;
import com.learningJWT.LearningTemplate.Enum.SubscriptionChangeMode;
import com.learningJWT.LearningTemplate.Model.Plan;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Model.StudentSubscription;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentSubscriptionDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentSubscriptionService {

    /** Called once when a student is created / first gets a plan. Also creates the linked Fee row. */
    StudentSubscription createInitialSubscription(Student student, Plan plan, LocalDate startDate) throws Exception;

    /**
     * Change (renew/upgrade) a student's plan. Freezes the current active subscription,
     * creates a new cycle, creates a new Fee invoice, and updates Student.plan.
     */
    StudentSubscription changePlan(Long studentId, Long newPlanId, SubscriptionChangeMode mode) throws Exception;

    Optional<StudentSubscription> getActiveSubscription(Long studentId);

    List<StudentSubscription> getSubscriptionHistory(Long studentId);

    /** ACTIVE / EXPIRING_SOON (cycleEnd within 3 days) / EXPIRED, computed at read time. */
    StudentSubscriptionStatus computeDisplayStatus(StudentSubscription sub);

    /**
     * Used by seat-allocation flows: if the student has no active subscription yet, creates
     * the initial one for {@code plan}; if they have an active subscription for a DIFFERENT
     * plan, extends/renews into the new plan (EXTEND mode); if it's already the same active
     * plan, does nothing and returns the existing cycle (no duplicate Fee rows).
     */
    StudentSubscription ensureSubscriptionForAllocation(Student student, Plan plan) throws Exception;

    StudentSubscriptionDTO toDTO(StudentSubscription sub);

    /** Fills subscription-derived fields (cycleStart/cycleEnd/status/etc) onto an already-built StudentDTO. */
    void enrichStudentDTO(com.learningJWT.LearningTemplate.Paylod.DTO.StudentDTO dto, Long studentId);
}
