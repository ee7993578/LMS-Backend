package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.SeatStatus;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String seatName;
    @Column(nullable = false)
    private String location;
    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    @ManyToOne
    @JoinColumn(name = "library_id")
    private Library library;

    @OneToMany(mappedBy = "seat")
    private List<Student> students;


    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedAt;


    @PrePersist
    public void createSeat() {
        this.createdAt = LocalDateTime.now();
    }
    @PreUpdate
    public void updateSeat() {
        this.updatedAt = LocalDateTime.now();
    }
}
