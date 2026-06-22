package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByLibraryId(Long libraryId);
    List<Slot> findByLibraryIdAndPlanId(Long libraryId, Long planId);
    void deleteByLibraryIdAndPlanId(Long libraryId, Long planId);
}
