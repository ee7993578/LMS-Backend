package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.FeeDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.PaymentProofDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PaymentProofService {

    /** Student submits a payment proof. screenshotFile and description are both optional (at least one expected, not enforced as mandatory). */
    PaymentProofDTO submitProof(MultipartFile screenshotFile, String description, Double amountClaimed) throws Exception;

    /** Student's own proof history. */
    List<PaymentProofDTO> getMyProofs() throws Exception;

    /** Admin: all proofs for their library, newest first. */
    List<PaymentProofDTO> getLibraryProofs() throws Exception;

    /** Admin: only pending proofs for their library. */
    List<PaymentProofDTO> getPendingProofs() throws Exception;

    /**
     * Admin verifies a proof: marks it VERIFIED and applies the fee update
     * (same effect as the existing fee-deposit flow) using the given FeeDTO payload.
     */
    FeeDTO verifyProof(Long proofId, FeeDTO feeDTO) throws Exception;

    /** Admin rejects a proof with an optional note shown back to the student. */
    PaymentProofDTO rejectProof(Long proofId, String adminNote) throws Exception;
}
