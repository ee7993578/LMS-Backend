package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    List<Plan> findByLibraryId(Long libraryId);

}
