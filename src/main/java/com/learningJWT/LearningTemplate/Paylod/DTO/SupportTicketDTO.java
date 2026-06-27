package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.TicketStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SupportTicketDTO {
    private Long id;
    private String subject;
    private String description;
    private TicketStatus status;
    private String priority;
    private String libraryName;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private List<TicketReplyDTO> replies;
}
