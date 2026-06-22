package com.learningJWT.LearningTemplate.Paylod.Response;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String jwt ;
    private String message ;



}
