package com.learningJWT.LearningTemplate.Model;

import com.learningJWT.LearningTemplate.Enum.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false,unique = true)
    private String username;
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role; // SUPERADMIN, LIBRARY_ADMIN, STUDENT

    @ManyToOne
    private Library library; // null for SuperAdmin

    private  String phone;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;
    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedAt;
    @Column(columnDefinition = "datetime")
    private LocalDateTime lastlogin;



}
