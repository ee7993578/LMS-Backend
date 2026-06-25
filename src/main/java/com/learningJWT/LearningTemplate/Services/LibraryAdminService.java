package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryPlanDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryUsageDTO;
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

    /** Library/subscription/usage status for the logged-in admin's own library — drives the
     *  trial/grace/expired banners and the "Current Students / Plan Limit / Grace Usage" panel. */
    LibraryUsageDTO getMySubscriptionStatus() throws Exception;

    /** All active SaaS subscription plans (LibraryPlan, not the student-facing Plan) that the
     *  library admin can browse and request to switch to. */
    List<LibraryPlanDTO> getPlanCatalog() throws Exception;
}
