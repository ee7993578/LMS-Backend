package com.learningJWT.LearningTemplate.Paylod.Request;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class FeeRequest {
    @NotNull(message = "Student ID required")
    private Long studentId;
    @Min(value = 1, message = "Month ID 1-12")
    @Max(value = 12, message = "Month ID 1-12")
    private int monthId;
    @DecimalMin(value = "0.0", message = "Must be >= 0")
    private double payable;
    @DecimalMin(value = "0.0", message = "Must be >= 0")
    private double receive;
    @DecimalMin(value = "0.0", message = "Must be >= 0")
    private double concession;
    @DecimalMin(value = "0.0", message = "Must be >= 0")
    private double lateFee;
}
