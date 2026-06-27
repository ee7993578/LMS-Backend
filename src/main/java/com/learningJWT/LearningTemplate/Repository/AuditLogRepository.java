package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByLibraryIdOrderByPerformedAtDesc(Long libraryId);
    Page<AuditLog> findByLibraryIdOrderByPerformedAtDesc(Long libraryId, Pageable pageable);
    List<AuditLog> findTop200ByOrderByPerformedAtDesc();
}
