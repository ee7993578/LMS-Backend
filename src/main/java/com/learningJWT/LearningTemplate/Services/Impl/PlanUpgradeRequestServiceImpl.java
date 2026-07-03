package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Enum.PlanRequestStatus;
import com.learningJWT.LearningTemplate.Enum.RazorpayPaymentStatus;
import com.learningJWT.LearningTemplate.Enum.RazorpayPurpose;
import com.learningJWT.LearningTemplate.Mapper.PlanUpgradeRequestMapper;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import com.learningJWT.LearningTemplate.Model.PlanUpgradeRequest;
import com.learningJWT.LearningTemplate.Model.RazorpayPayment;
import com.learningJWT.LearningTemplate.Model.Subscription;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.PlanUpgradeRequestDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.RazorpayOrderResponseDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.RazorpayVerifyRequestDTO;
import com.learningJWT.LearningTemplate.Repository.LibraryPlanRepository;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Repository.PlanUpgradeRequestRepository;
import com.learningJWT.LearningTemplate.Repository.RazorpayPaymentRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.PlanUpgradeRequestService;
import com.learningJWT.LearningTemplate.Services.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanUpgradeRequestServiceImpl implements PlanUpgradeRequestService {

    private final PlanUpgradeRequestRepository requestRepository;
    private final LibraryPlanRepository libraryPlanRepository;
    private final LibraryRepository libraryRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RazorpayPaymentRepository razorpayPaymentRepository;
    private final RazorpayClient razorpayClient;

    private User getLoggedInAdmin() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new Exception("Logged-in user not found"));
        }
        throw new Exception("No authenticated user found");
    }

    @Override
    public PlanUpgradeRequestDTO createRequest(Long requestedPlanId, String note) throws Exception {
        User admin = getLoggedInAdmin();
        Library library = admin.getLibrary();
        if (library == null) {
            throw new Exception("Admin is not associated with a library");
        }

        LibraryPlan requestedPlan = libraryPlanRepository.findById(requestedPlanId)
                .orElseThrow(() -> new Exception("Plan not found"));

        if (library.getLibraryPlan() != null && library.getLibraryPlan().getPlanId().equals(requestedPlanId)) {
            throw new Exception("You are already on this plan");
        }

        if (requestRepository.existsByLibraryIdAndStatus(library.getId(), PlanRequestStatus.PENDING)) {
            throw new Exception("You already have a pending plan change request. Please wait for it to be resolved.");
        }

        PlanUpgradeRequest request = PlanUpgradeRequest.builder()
                .library(library)
                .currentPlan(library.getLibraryPlan())
                .requestedPlan(requestedPlan)
                .note(note)
                .status(PlanRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        requestRepository.save(request);
        return PlanUpgradeRequestMapper.toDTO(request);
    }

    @Override
    public List<PlanUpgradeRequestDTO> getMyRequests() throws Exception {
        User admin = getLoggedInAdmin();
        Library library = admin.getLibrary();
        if (library == null) {
            throw new Exception("Admin is not associated with a library");
        }
        return requestRepository.findByLibraryIdOrderByCreatedAtDesc(library.getId()).stream()
                .map(PlanUpgradeRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlanUpgradeRequestDTO> getAllRequests(String status) throws Exception {
        List<PlanUpgradeRequest> requests;
        if (status != null && !status.isBlank()) {
            requests = requestRepository.findByStatusOrderByCreatedAtDesc(PlanRequestStatus.valueOf(status.toUpperCase()));
        } else {
            requests = requestRepository.findAllByOrderByCreatedAtDesc();
        }
        return requests.stream().map(PlanUpgradeRequestMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public PlanUpgradeRequestDTO approveRequest(Long requestId, String resolutionNote) throws Exception {
        PlanUpgradeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Request not found"));

        if (request.getStatus() != PlanRequestStatus.PENDING) {
            throw new Exception("This request has already been resolved");
        }

        applyPlanSwitch(request.getLibrary(), request.getRequestedPlan());

        request.setStatus(PlanRequestStatus.APPROVED);
        request.setResolutionNote(resolutionNote);
        request.setResolvedAt(LocalDateTime.now());
        requestRepository.save(request);

        return PlanUpgradeRequestMapper.toDTO(request);
    }

    /** Actually switches library + subscription to newPlan. Shared by SuperAdmin manual
     *  approval and by the Razorpay-paid auto-approval path so both apply the change identically. */
    private void applyPlanSwitch(Library library, LibraryPlan newPlan) {
        library.setLibraryPlan(newPlan);
        library.setGracePeriodStartedAt(null); // grace math depends on the plan; reset and let it re-evaluate
        libraryRepository.save(library);

        Subscription subscription = subscriptionRepository.findByLibraryId(library.getId());
        if (subscription != null) {
            subscription.setLibraryPlan(newPlan);
            subscription.setPlanName(newPlan.getPlanName());
            subscription.setStudentLimit(newPlan.getNoOfStudent());
            subscription.setBufferAllowed(newPlan.getBufferStudent());
            subscription.setBufferExpiryDays(newPlan.getGracePeriodDays() != null ? newPlan.getGracePeriodDays() : 3);
            subscription.setPricePerMonth(newPlan.getPlanPrice());
            subscriptionRepository.save(subscription);
        }
    }

    @Override
    public PlanUpgradeRequestDTO rejectRequest(Long requestId, String resolutionNote) throws Exception {
        PlanUpgradeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new Exception("Request not found"));

        if (request.getStatus() != PlanRequestStatus.PENDING) {
            throw new Exception("This request has already been resolved");
        }

        // Reject = decision recorded only. Library's plan/subscription are left completely
        // untouched, as required.
        request.setStatus(PlanRequestStatus.REJECTED);
        request.setResolutionNote(resolutionNote);
        request.setResolvedAt(LocalDateTime.now());
        requestRepository.save(request);

        return PlanUpgradeRequestMapper.toDTO(request);
    }

    // ===================== Razorpay-backed self-serve upgrade =====================

    @Override
    @Transactional
    public RazorpayOrderResponseDTO initiateUpgrade(Long requestedPlanId, String note) throws Exception {
        User admin = getLoggedInAdmin();
        Library library = admin.getLibrary();
        if (library == null) {
            throw new Exception("Admin is not associated with a library");
        }

        LibraryPlan requestedPlan = libraryPlanRepository.findById(requestedPlanId)
                .orElseThrow(() -> new Exception("Plan not found"));

        if (library.getLibraryPlan() != null && library.getLibraryPlan().getPlanId().equals(requestedPlanId)) {
            throw new Exception("You are already on this plan");
        }

        if (requestRepository.existsByLibraryIdAndStatus(library.getId(), PlanRequestStatus.PENDING)) {
            throw new Exception("You already have a pending plan change request. Please wait for it to be resolved, or cancel it first.");
        }

        PlanUpgradeRequest request = PlanUpgradeRequest.builder()
                .library(library)
                .currentPlan(library.getLibraryPlan())
                .requestedPlan(requestedPlan)
                .note(note)
                .status(PlanRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        request = requestRepository.save(request);

        boolean isFree = requestedPlan.getPlanPrice() == null || requestedPlan.getPlanPrice() <= 0.0;
        if (isFree) {
            // Free plan switch — no payment needed. Leave it PENDING for SuperAdmin to approve,
            // exactly like the original manual flow (e.g. downgrading to a free tier shouldn't
            // require SuperAdmin sign-off to be skipped just because no money changed hands).
            return RazorpayOrderResponseDTO.builder()
                    .requiresPayment(false)
                    .result(PlanUpgradeRequestMapper.toDTO(request))
                    .build();
        }

        long amountPaise = Math.round(requestedPlan.getPlanPrice() * 100);

        RazorpayPayment payment = RazorpayPayment.builder()
                .purpose(RazorpayPurpose.LIBRARY_PLAN_UPGRADE)
                .status(RazorpayPaymentStatus.CREATED)
                .amountPaise(amountPaise)
                .currency("INR")
                .planId(requestedPlan.getPlanId())
                .libraryId(library.getId())
                .planUpgradeRequestId(request.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        payment = razorpayPaymentRepository.save(payment);

        String orderId;
        try {
            orderId = razorpayClient.createOrder(amountPaise, "INR", "upgrade-" + payment.getId());
        } catch (Exception e) {
            payment.setStatus(RazorpayPaymentStatus.FAILED);
            payment.setNote("Order creation failed: " + e.getMessage());
            payment.setUpdatedAt(LocalDateTime.now());
            razorpayPaymentRepository.save(payment);
            // Don't leave a dangling PENDING request behind if we couldn't even create the order.
            request.setStatus(PlanRequestStatus.REJECTED);
            request.setResolutionNote("Payment gateway error — please try again.");
            request.setResolvedAt(LocalDateTime.now());
            requestRepository.save(request);
            throw e;
        }

        payment.setRazorpayOrderId(orderId);
        razorpayPaymentRepository.save(payment);

        return RazorpayOrderResponseDTO.builder()
                .requiresPayment(true)
                .razorpayOrderId(orderId)
                .keyId(razorpayClient.getKeyId())
                .amountPaise(amountPaise)
                .currency("INR")
                .paymentRecordId(payment.getId())
                .planName(requestedPlan.getPlanName())
                .result(PlanUpgradeRequestMapper.toDTO(request))
                .build();
    }

    @Override
    @Transactional
    public PlanUpgradeRequestDTO verifyUpgrade(RazorpayVerifyRequestDTO body) throws Exception {
        if (body.getPaymentRecordId() == null) {
            throw new Exception("Missing payment record id");
        }

        RazorpayPayment payment = razorpayPaymentRepository.findById(body.getPaymentRecordId())
                .orElseThrow(() -> new Exception("Payment record not found"));

        if (payment.getPurpose() != RazorpayPurpose.LIBRARY_PLAN_UPGRADE) {
            throw new Exception("Invalid payment record for this operation");
        }

        PlanUpgradeRequest request = requestRepository.findById(payment.getPlanUpgradeRequestId())
                .orElseThrow(() -> new Exception("Plan upgrade request not found"));

        // Idempotent — webhook may have already finalized this.
        if (payment.getStatus() == RazorpayPaymentStatus.PAID) {
            return PlanUpgradeRequestMapper.toDTO(request);
        }

        if (request.getStatus() != PlanRequestStatus.PENDING) {
            throw new Exception("This request has already been resolved");
        }

        if (payment.getRazorpayOrderId() == null || !payment.getRazorpayOrderId().equals(body.getRazorpayOrderId())) {
            throw new Exception("Order id mismatch");
        }

        boolean valid = razorpayClient.verifyCheckoutSignature(
                body.getRazorpayOrderId(), body.getRazorpayPaymentId(), body.getRazorpaySignature());

        if (!valid) {
            payment.setStatus(RazorpayPaymentStatus.FAILED);
            payment.setNote("Signature verification failed");
            payment.setUpdatedAt(LocalDateTime.now());
            razorpayPaymentRepository.save(payment);
            throw new Exception("Payment verification failed. If money was deducted, it will be auto-refunded; please contact support otherwise.");
        }

        return finalizeUpgrade(payment, request, body.getRazorpayPaymentId(), body.getRazorpaySignature(),
                "Auto-approved — payment verified via Razorpay Checkout signature.");
    }

    @Override
    @Transactional
    public void finalizeUpgradeTrusted(String razorpayOrderId, String razorpayPaymentId) throws Exception {
        RazorpayPayment payment = razorpayPaymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null || payment.getPurpose() != RazorpayPurpose.LIBRARY_PLAN_UPGRADE) return;
        if (payment.getStatus() == RazorpayPaymentStatus.PAID) return; // already done — idempotent

        PlanUpgradeRequest request = requestRepository.findById(payment.getPlanUpgradeRequestId()).orElse(null);
        if (request == null || request.getStatus() != PlanRequestStatus.PENDING) return;

        finalizeUpgrade(payment, request, razorpayPaymentId, null,
                "Auto-approved — payment confirmed via Razorpay webhook.");
    }

    /** Shared by the synchronous /verify call and the webhook fallback. Must only be called
     *  once signature trust has already been established by the caller. */
    private PlanUpgradeRequestDTO finalizeUpgrade(RazorpayPayment payment, PlanUpgradeRequest request,
                                                   String paymentId, String signature, String resolutionNote) {
        applyPlanSwitch(request.getLibrary(), request.getRequestedPlan());

        request.setStatus(PlanRequestStatus.APPROVED);
        request.setResolutionNote(resolutionNote);
        request.setResolvedAt(LocalDateTime.now());
        requestRepository.save(request);

        payment.setStatus(RazorpayPaymentStatus.PAID);
        payment.setRazorpayPaymentId(paymentId);
        if (signature != null) payment.setRazorpaySignature(signature);
        payment.setUpdatedAt(LocalDateTime.now());
        razorpayPaymentRepository.save(payment);

        return PlanUpgradeRequestMapper.toDTO(request);
    }

    @Override
    @Transactional
    public void cancelUpgrade(Long paymentRecordId) throws Exception {
        RazorpayPayment payment = razorpayPaymentRepository.findById(paymentRecordId)
                .orElseThrow(() -> new Exception("Payment record not found"));

        if (payment.getStatus() == RazorpayPaymentStatus.CREATED) {
            payment.setStatus(RazorpayPaymentStatus.CANCELLED);
            payment.setUpdatedAt(LocalDateTime.now());
            razorpayPaymentRepository.save(payment);
        }

        if (payment.getPlanUpgradeRequestId() != null) {
            requestRepository.findById(payment.getPlanUpgradeRequestId()).ifPresent(request -> {
                if (request.getStatus() == PlanRequestStatus.PENDING) {
                    request.setStatus(PlanRequestStatus.REJECTED);
                    request.setResolutionNote("Cancelled by library admin before completing payment.");
                    request.setResolvedAt(LocalDateTime.now());
                    requestRepository.save(request);
                }
            });
        }
    }
}
