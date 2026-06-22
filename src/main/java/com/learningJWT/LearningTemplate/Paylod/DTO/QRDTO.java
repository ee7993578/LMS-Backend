package com.learningJWT.LearningTemplate.Paylod.DTO;
import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Model.Library;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QRDTO {
    private Long qrId;
    private String qrCodeValue;
    private Library library;
    private Long libraryId;
    private Status status;
}
