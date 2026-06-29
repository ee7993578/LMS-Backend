package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.FeeAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeeAuditLogRepository extends JpaRepository<FeeAuditLog, Long> {
    List<FeeAuditLog> findByLibraryIdOrderByPerformedAtDesc(Long libraryId);
    List<FeeAuditLog> findByStudentIdOrderByPerformedAtDesc(Long studentId);

    @Query("SELECT f FROM FeeAuditLog f WHERE f.fee.feeId = :feeId ORDER BY f.performedAt DESC")
    List<FeeAuditLog> findByFeeIdOrderByPerformedAtDesc(@Param("feeId") Long feeId);
}