package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Enum.RazorpayPaymentStatus;
import com.learningJWT.LearningTemplate.Enum.RazorpayPurpose;
import com.learningJWT.LearningTemplate.Model.RazorpayPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RazorpayPaymentRepository extends JpaRepository<RazorpayPayment, Long> {

    Optional<RazorpayPayment> findByRazorpayOrderId(String razorpayOrderId);

    List<RazorpayPayment> findByPlanUpgradeRequestIdAndStatus(Long planUpgradeRequestId, RazorpayPaymentStatus status);

    boolean existsByLibraryIdAndPurposeAndStatus(Long libraryId, RazorpayPurpose purpose, RazorpayPaymentStatus status);
}
