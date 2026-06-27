package com.learningJWT.LearningTemplate.Paylod.DTO;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketReplyDTO {
    private Long id;
    private String message;
    private String repliedBy;
    private boolean isAdminReply;
    private LocalDateTime createdAt;
}
