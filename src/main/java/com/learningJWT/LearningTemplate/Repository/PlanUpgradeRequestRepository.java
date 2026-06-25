package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Enum.PlanRequestStatus;
import com.learningJWT.LearningTemplate.Model.PlanUpgradeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanUpgradeRequestRepository extends JpaRepository<PlanUpgradeRequest, Long> {
    List<PlanUpgradeRequest> findByLibraryIdOrderByCreatedAtDesc(Long libraryId);
    List<PlanUpgradeRequest> findByStatusOrderByCreatedAtDesc(PlanRequestStatus status);
    List<PlanUpgradeRequest> findAllByOrderByCreatedAtDesc();
    boolean existsByLibraryIdAndStatus(Long libraryId, PlanRequestStatus status);
}
