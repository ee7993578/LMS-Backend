package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Enum.StudentSubscriptionStatus;
import com.learningJWT.LearningTemplate.Model.StudentSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentSubscriptionRepository extends JpaRepository<StudentSubscription, Long> {

    Optional<StudentSubscription> findFirstByStudentIdAndStatusOrderByCycleStartDesc(
            Long studentId, StudentSubscriptionStatus status);

    List<StudentSubscription> findByStudentIdOrderByCycleStartDesc(Long studentId);

    List<StudentSubscription> findByStatus(StudentSubscriptionStatus status);

    List<StudentSubscription> findByLibraryIdAndStatus(Long libraryId, StudentSubscriptionStatus status);

    @Query("SELECT s FROM StudentSubscription s WHERE s.student.id = :studentId ORDER BY s.cycleStart DESC")
    List<StudentSubscription> findHistoryForStudent(@Param("studentId") Long studentId);

    boolean existsByStudentId(Long studentId);
}
