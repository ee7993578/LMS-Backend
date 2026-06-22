package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Exception.UserException;
import com.learningJWT.LearningTemplate.Paylod.DTO.UserDto;
import com.learningJWT.LearningTemplate.Paylod.Response.AuthResponse;

public interface AuthService {

    AuthResponse signup(UserDto userDto) throws UserException;
    AuthResponse login(UserDto userDto) throws UserException;

}