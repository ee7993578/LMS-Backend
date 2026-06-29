package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.NotificationChannel;
import com.learningJWT.LearningTemplate.Enum.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_user", columnList = "user_id"),
    @Index(name = "idx_notif_library", columnList = "library_id"),
    @Index(name = "idx_notif_read",    columnList = "is_read"),
    @Index(name = "idx_notif_created", columnList = "created_at"),
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id")
    private Library library;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String message;

    private boolean isRead;
    private boolean isArchived;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime readAt;

    /** Optional deep-link URL shown in notification */
    private String actionUrl;
}
