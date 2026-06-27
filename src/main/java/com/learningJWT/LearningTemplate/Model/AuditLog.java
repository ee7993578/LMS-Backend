package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_library", columnList = "library_id"),
    @Index(name = "idx_audit_user", columnList = "performed_by_user_id"),
    @Index(name = "idx_audit_ts", columnList = "performed_at"),
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id")
    private User performedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_id")
    private Library library;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AuditAction action;

    /** e.g. "Student", "Fee", "Subscription" */
    private String entityType;
    private String entityId;

    @Column(length = 4000)
    private String oldValue;

    @Column(length = 4000)
    private String newValue;

    @Column(length = 500)
    private String description;

    private String ipAddress;
    private String userAgent;

    @Column(columnDefinition = "datetime", nullable = false)
    private LocalDateTime performedAt;
}
