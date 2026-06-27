package com.learningJWT.LearningTemplate.Paylod.Request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class CreateStudentRequest {
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String fullName;
    @Email(message = "Invalid email format")
    private String email;
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;
    private String admissionNumber;
    @NotNull(message = "Seat ID is required")
    private Long seatId;
    @NotNull(message = "Plan ID is required")
    private Long planId;
}
