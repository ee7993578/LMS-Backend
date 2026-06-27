package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.AnnouncementType;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnouncementDTO {
    private Long id;
    private String title;
    private String content;
    private AnnouncementType type;
    private boolean active;
    private String targetLibraryName;
    private Long targetLibraryId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
