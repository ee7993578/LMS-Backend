package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.*;
import java.time.LocalDateTime;

/** One row in the Library Admin "Sent Notifications" management screen. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SentNotificationDTO {
    /** For broadcasts: the broadcastGroupId (used to delete all copies together).
     *  For individual notifications: the row's own id (as a string). */
    private String key;
    private String title;
    private String message;
    private String scope;          // "LIBRARY" or "STUDENT"
    private String studentName;    // only set when scope == STUDENT
    private int recipientCount;    // only meaningful when scope == LIBRARY
    private LocalDateTime createdAt;
    private LocalDateTime dashboardExpiresAt; // null for STUDENT scope
    private boolean dashboardActive;          // true while still shown on student dashboards
}
