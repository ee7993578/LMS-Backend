package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.AnnouncementType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Announcement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private AnnouncementType type;

    /** null = all libraries, non-null = specific library */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_library_id")
    private Library targetLibrary;

    private boolean active;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime expiresAt;
}
