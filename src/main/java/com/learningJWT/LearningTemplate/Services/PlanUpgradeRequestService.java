package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.PlanUpgradeRequestDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.RazorpayOrderResponseDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.RazorpayVerifyRequestDTO;

import java.util.List;

public interface PlanUpgradeRequestService {

    /** Library admin creates a request to switch to a different plan. Used directly only for
     *  free-plan switches (no payment needed); paid switches go through initiateUpgrade/verifyUpgrade. */
    PlanUpgradeRequestDTO createRequest(Long requestedPlanId, String note) throws Exception;

    /** Library admin views their own request history. */
    List<PlanUpgradeRequestDTO> getMyRequests() throws Exception;

    /** SuperAdmin: all requests, optionally filtered by status (PENDING/APPROVED/REJECTED). */
    List<PlanUpgradeRequestDTO> getAllRequests(String status) throws Exception;

    /** SuperAdmin approves — actually switches the library to the requested plan. */
    PlanUpgradeRequestDTO approveRequest(Long requestId, String resolutionNote) throws Exception;

    /** SuperAdmin rejects — library plan is left untouched, decision is just recorded. */
    PlanUpgradeRequestDTO rejectRequest(Long requestId, String resolutionNote) throws Exception;

    // ===================== Razorpay-backed self-serve upgrade =====================

    /** Library admin wants to switch to requestedPlanId. If that plan is free, behaves like
     *  createRequest() above (no payment) and the result is already PENDING for SuperAdmin.
     *  If the plan has a price, creates a PENDING PlanUpgradeRequest + a Razorpay order and
     *  returns checkout details — the plan switch is only applied once payment is verified. */
    RazorpayOrderResponseDTO initiateUpgrade(Long requestedPlanId, String note) throws Exception;

    /** Verifies the Razorpay signature for a plan-upgrade payment and, if valid, immediately
     *  applies the plan switch (same effect as SuperAdmin clicking "approve") — no manual
     *  SuperAdmin step needed for paid upgrades. */
    PlanUpgradeRequestDTO verifyUpgrade(RazorpayVerifyRequestDTO body) throws Exception;

    /** Library admin closed the Razorpay checkout without paying — cancels the pending
     *  request + payment record so it doesn't block creating a new request. */
    void cancelUpgrade(Long paymentRecordId) throws Exception;

    /** Same finalization as verifyUpgrade, but called from the Razorpay webhook — which is
     *  already authenticated via the webhook secret, not the checkout signature — so the
     *  checkout signature is not re-checked here. Idempotent against verifyUpgrade. */
    void finalizeUpgradeTrusted(String razorpayOrderId, String razorpayPaymentId) throws Exception;
}

