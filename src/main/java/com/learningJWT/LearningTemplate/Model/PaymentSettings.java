package com.learningJWT.LearningTemplate.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "library_id", unique = true, nullable = false)
    private Library library;

    /** Relative path under the uploads dir, e.g. "qr/3_1719999999.png" */
    private String qrImagePath;

    private String upiId;

    private String phoneNumber;

    @Column(length = 1000)
    private String description;

    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedAt;
}
