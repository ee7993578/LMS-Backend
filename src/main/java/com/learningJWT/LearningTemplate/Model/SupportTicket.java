package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "support_ticket", indexes = {
    @Index(name = "idx_ticket_library", columnList = "library_id"),
    @Index(name = "idx_ticket_status", columnList = "status"),
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicket {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id", nullable = false)
    private Library library;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    private String subject;

    @Column(length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TicketStatus status;

    private String priority; // LOW, MEDIUM, HIGH

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<TicketReply> replies = new ArrayList<>();
}
