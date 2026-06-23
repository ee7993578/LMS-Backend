package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Enum.ProofStatus;
import com.learningJWT.LearningTemplate.Model.PaymentProof;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentProofRepository extends JpaRepository<PaymentProof, Long> {
    List<PaymentProof> findByLibraryIdOrderBySubmittedAtDesc(Long libraryId);
    List<PaymentProof> findByLibraryIdAndStatusOrderBySubmittedAtDesc(Long libraryId, ProofStatus status);
    List<PaymentProof> findByStudentIdOrderBySubmittedAtDesc(Long studentId);
}
