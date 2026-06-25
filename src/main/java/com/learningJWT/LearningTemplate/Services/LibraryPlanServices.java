package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryPlanDTO;
import com.learningJWT.LearningTemplate.Paylod.Response.ApiResponse;

import java.util.List;

public interface LibraryPlanServices {

    LibraryPlanDTO createPlan(LibraryPlanDTO libraryPlanDTO) throws Exception;
    LibraryPlanDTO updatePlan(Long id,LibraryPlanDTO libraryPlanDTO) throws Exception;
    ApiResponse deletePlan(Long id) throws Exception;
    LibraryPlanDTO getPlan(Long id) throws Exception;
    List<LibraryPlanDTO> getAllPlans() throws Exception;
    List<LibraryPlanDTO> getActivePlans() throws Exception;
    LibraryPlanDTO setPlanActive(Long id, boolean active) throws Exception;

}
