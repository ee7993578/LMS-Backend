package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndIsArchivedFalseOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndIsReadFalseAndIsArchivedFalse(Long userId);
    @Modifying
    @Query("UPDATE Notification n SET n.isRead=true WHERE n.user.id=:uid AND n.isRead=false")
    void markAllReadByUser(@Param("uid") Long userId);
    List<Notification> findByLibraryIdOrderByCreatedAtDesc(Long libraryId);
}
