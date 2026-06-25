package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface LibraryPlanRepository extends JpaRepository<LibraryPlan,Long> {
    List<LibraryPlan> findByIsActiveTrue();
}
