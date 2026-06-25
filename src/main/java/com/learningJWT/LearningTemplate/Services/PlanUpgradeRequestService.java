package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.PlanUpgradeRequestDTO;

import java.util.List;

public interface PlanUpgradeRequestService {

    /** Library admin creates a request to switch to a different plan. */
    PlanUpgradeRequestDTO createRequest(Long requestedPlanId, String note) throws Exception;

    /** Library admin views their own request history. */
    List<PlanUpgradeRequestDTO> getMyRequests() throws Exception;

    /** SuperAdmin: all requests, optionally filtered by status (PENDING/APPROVED/REJECTED). */
    List<PlanUpgradeRequestDTO> getAllRequests(String status) throws Exception;

    /** SuperAdmin approves — actually switches the library to the requested plan. */
    PlanUpgradeRequestDTO approveRequest(Long requestId, String resolutionNote) throws Exception;

    /** SuperAdmin rejects — library plan is left untouched, decision is just recorded. */
    PlanUpgradeRequestDTO rejectRequest(Long requestId, String resolutionNote) throws Exception;
}
