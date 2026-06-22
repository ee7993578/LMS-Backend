package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.PlanDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentDTO;

import java.util.List;

public interface LibraryAdminService {

    StudentDTO createStudent(StudentDTO dto) throws Exception;

    StudentDTO updateStudent(Long studentId, StudentDTO dto) throws Exception;

    void deleteStudent(Long studentId) throws Exception;

    List<StudentDTO> getAllStudents();

    PlanDTO createPlan(PlanDTO dto) throws Exception;

    PlanDTO updatePlan(Long planId, PlanDTO dto) throws Exception;

    void deletePlan(Long planId) throws Exception;

    List<PlanDTO> getAllPlans() throws Exception;

    PlanDTO getPlanById(Long id) throws Exception;

    String generateQR() throws Exception;

    String getQR() throws Exception;
}
