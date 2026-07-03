package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface LibraryPlanRepository extends JpaRepository<LibraryPlan,Long> {
    List<LibraryPlan> findByIsActiveTrue();

    /** The free/starter plan — lowest planOrder among active plans. Used to decide whether
     *  a library-signup or plan-upgrade requires Razorpay payment (order 1 = free, no payment). */
    java.util.Optional<LibraryPlan> findFirstByIsActiveTrueOrderByPlanOrderAsc();
}
