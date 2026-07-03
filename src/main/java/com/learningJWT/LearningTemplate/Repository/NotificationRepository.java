package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndIsArchivedFalseOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndIsReadFalseAndIsArchivedFalse(Long userId);
    @Modifying
    @Query("UPDATE Notification n SET n.isRead=true WHERE n.user.id=:uid AND n.isRead=false")
    void markAllReadByUser(@Param("uid") Long userId);
    List<Notification> findByLibraryIdOrderByCreatedAtDesc(Long libraryId);

    /** Student dashboard banner: this student's still-active library-wide broadcast(s). */
    @Query("SELECT n FROM Notification n WHERE n.user.id=:uid AND n.broadcastGroupId IS NOT NULL " +
           "AND n.dashboardExpiresAt IS NOT NULL AND n.dashboardExpiresAt > :now ORDER BY n.createdAt DESC")
    List<Notification> findActiveDashboardBroadcastsForUser(@Param("uid") Long userId, @Param("now") LocalDateTime now);

    /** All notifications an admin has ever sent (broadcast + individual) for the "sent history" screen. */
    @Query("SELECT n FROM Notification n WHERE n.library.id=:libId AND n.type='ADMIN_MESSAGE' ORDER BY n.createdAt DESC")
    List<Notification> findAdminSentForLibrary(@Param("libId") Long libraryId);

    List<Notification> findByBroadcastGroupId(String broadcastGroupId);

    @Modifying
    void deleteByBroadcastGroupId(String broadcastGroupId);

    /** Used by the monthly cleanup job — wipes every notification for one library. */
    @Modifying
    void deleteByLibraryId(Long libraryId);
}
