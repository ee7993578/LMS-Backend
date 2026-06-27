package com.learningJWT.LearningTemplate.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_reply")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketReply {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replied_by_user_id")
    private User repliedBy;

    @Column(length = 5000, nullable = false)
    private String message;

    private boolean isAdminReply;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;
}
