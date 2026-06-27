package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    @Query("SELECT a FROM Announcement a WHERE a.active=true AND (a.expiresAt IS NULL OR a.expiresAt > :now) AND (a.targetLibrary IS NULL OR a.targetLibrary.id = :libId) ORDER BY a.createdAt DESC")
    List<Announcement> findActiveForLibrary(@Param("libId") Long libraryId, @Param("now") LocalDateTime now);
    List<Announcement> findAllByOrderByCreatedAtDesc();
}
