package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.NotificationType;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private boolean isArchived;
    private String actionUrl;
    private LocalDateTime createdAt;
}
