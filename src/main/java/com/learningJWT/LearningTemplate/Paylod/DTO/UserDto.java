package com.learningJWT.LearningTemplate.Paylod.DTO;

import com.learningJWT.LearningTemplate.Enum.UserRole;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
@Data

public class UserDto {
    private Long id ;
    private String fullName ;

    private String username;

    private UserRole role;

    private String phone ;

    private String password;

    @Column(columnDefinition = "datetime")
    private LocalDateTime createdAt;
    @Column(columnDefinition = "datetime")
    private LocalDateTime updatedAt;
    @Column(columnDefinition = "datetime")
    private LocalDateTime lastlogin;
}
