package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QR {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long qrId;
    private String qrCodeValue;
    @ManyToOne
    @JoinColumn(name = "library_id")
    private Library library;
    private Status status;

}
