package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.FeeAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeAuditLogRepository extends JpaRepository<FeeAuditLog, Long> {
    List<FeeAuditLog> findByLibraryIdOrderByPerformedAtDesc(Long libraryId);
    List<FeeAuditLog> findByStudentIdOrderByPerformedAtDesc(Long studentId);
    List<FeeAuditLog> findByFee_FeeIdOrderByPerformedAtDesc(Long feeId);
}
