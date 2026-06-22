package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.UserDto;

public class UserMapper {
    public static UserDto toDto(User saveUser) {

        UserDto userDto  = new UserDto();
        userDto.setId(saveUser.getId());
        userDto.setFullName(saveUser.getFullName());
        userDto.setUsername(saveUser.getUsername());
        userDto.setPassword(saveUser.getPassword());
        userDto.setRole(saveUser.getRole());
        userDto.setCreatedAt(saveUser.getCreatedAt());
        userDto.setUpdatedAt(saveUser.getUpdatedAt());
        userDto.setLastlogin(saveUser.getLastlogin());

        return  userDto;
    }
}
