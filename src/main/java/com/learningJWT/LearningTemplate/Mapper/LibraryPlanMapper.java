package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryPlanDTO;

public class LibraryPlanMapper {

    public static LibraryPlanDTO toDto(LibraryPlan entity) {
       return LibraryPlanDTO
               .builder()
               .planId(entity.getPlanId())
               .planPrice(entity.getPlanPrice())
               .noOfStudent(entity.getNoOfStudent())
               .bufferStudent(entity.getBufferStudent())
               .planOrder(entity.getPlanOrder())
               .planName(entity.getPlanName())
               .noOfDays(entity.getNoOfDays())
               .build();
    }


    public static LibraryPlan toEntity(LibraryPlanDTO libraryPlanDTO) {
        return LibraryPlan
                .builder()
                .planId(libraryPlanDTO.getPlanId())
                .planPrice(libraryPlanDTO.getPlanPrice())
                .noOfStudent(libraryPlanDTO.getNoOfStudent())
                .bufferStudent(libraryPlanDTO.getBufferStudent())
                .planOrder(libraryPlanDTO.getPlanOrder())
                .planName(libraryPlanDTO.getPlanName())
                .noOfDays(libraryPlanDTO.getNoOfDays())
                .build();
    }

}
