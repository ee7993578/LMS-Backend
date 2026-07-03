package com.learningJWT.LearningTemplate.Services.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningJWT.LearningTemplate.Enum.RazorpayPaymentStatus;
import com.learningJWT.LearningTemplate.Enum.RazorpayPurpose;
import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import com.learningJWT.LearningTemplate.Model.RazorpayPayment;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.RazorpayOrderResponseDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.RazorpayVerifyRequestDTO;
import com.learningJWT.LearningTemplate.Repository.LibraryPlanRepository;
import com.learningJWT.LearningTemplate.Repository.RazorpayPaymentRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.LibrarySignupPaymentService;
import com.learningJWT.LearningTemplate.Services.SuperAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibrarySignupPaymentServiceImpl implements LibrarySignupPaymentService {

    private final LibraryPlanRepository libraryPlanRepository;
    private final UserRepository userRepository;
    private final RazorpayPaymentRepository razorpayPaymentRepository;
    private final RazorpayClient razorpayClient;
    private final SuperAdminService superAdminService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public RazorpayOrderResponseDTO initiateSignup(LibraryDTO dto) throws Exception {

        if (dto.getAdminUsername() == null || dto.getAdminUsername().isBlank()) {
            throw new Exception("Admin username is required!");
        }
        if (userRepository.existsByUsername(dto.getAdminUsername())) {
            throw new Exception("Admin username already exists!");
        }

        LibraryPlan chosenPlan = null;
        if (dto.getLibraryPlanId() != null) {
            chosenPlan = libraryPlanRepository.findById(dto.getLibraryPlanId())
                    .orElseThrow(() -> new Exception("Library plan not found"));
        }

        LibraryPlan freePlan = libraryPlanRepository.findFirstByIsActiveTrueOrderByPlanOrderAsc().orElse(null);

        // No plan chosen, OR the chosen plan IS the lowest-order (free/starter) plan -> no
        // payment needed, create the library right now exactly like before.
        boolean isFreePlanChoice = chosenPlan == null
                || (freePlan != null && freePlan.getPlanId().equals(chosenPlan.getPlanId()))
                || (chosenPlan.getPlanPrice() == null || chosenPlan.getPlanPrice() <= 0.0);

        if (isFreePlanChoice) {
            LibraryDTO created = superAdminService.createLibrary(dto);
            return RazorpayOrderResponseDTO.builder()
                    .requiresPayment(false)
                    .result(created)
                    .build();
        }

        // Paid plan -> stage the signup, create a Razorpay order, do NOT touch Library/User yet.
        long amountPaise = Math.round(chosenPlan.getPlanPrice() * 100);
        String stagedJson = objectMapper.writeValueAsString(dto);

        RazorpayPayment payment = RazorpayPayment.builder()
                .purpose(RazorpayPurpose.LIBRARY_SIGNUP)
                .status(RazorpayPaymentStatus.CREATED)
                .amountPaise(amountPaise)
                .currency("INR")
                .planId(chosenPlan.getPlanId())
                .stagedPayloadJson(stagedJson)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        // Save first to get an id we can use in the Razorpay receipt, then create the order.
        payment = razorpayPaymentRepository.save(payment);

        String orderId;
        try {
            orderId = razorpayClient.createOrder(amountPaise, "INR", "signup-" + payment.getId());
        } catch (Exception e) {
            payment.setStatus(RazorpayPaymentStatus.FAILED);
            payment.setNote("Order creation failed: " + e.getMessage());
            payment.setUpdatedAt(LocalDateTime.now());
            razorpayPaymentRepository.save(payment);
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
                .planName(chosenPlan.getPlanName())
                .build();
    }

    @Override
    @Transactional
    public LibraryDTO verifySignup(RazorpayVerifyRequestDTO body) throws Exception {
        if (body.getPaymentRecordId() == null) {
            throw new Exception("Missing payment record id");
        }

        RazorpayPayment payment = razorpayPaymentRepository.findById(body.getPaymentRecordId())
                .orElseThrow(() -> new Exception("Payment record not found"));

        if (payment.getPurpose() != RazorpayPurpose.LIBRARY_SIGNUP) {
            throw new Exception("Invalid payment record for this operation");
        }

        // Already finalized (e.g. webhook beat us to it) — return success idempotently instead
        // of creating a duplicate library.
        if (payment.getStatus() == RazorpayPaymentStatus.PAID && payment.getLibraryId() != null) {
            return superAdminService.findById(payment.getLibraryId());
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

        return finalizeAndCreateLibrary(payment, body.getRazorpayPaymentId(), body.getRazorpaySignature(),
                "Verified via Razorpay Checkout signature");
    }

    @Override
    @Transactional
    public void finalizeSignupTrusted(String razorpayOrderId, String razorpayPaymentId) throws Exception {
        RazorpayPayment payment = razorpayPaymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElse(null);
        if (payment == null) {
            log.warn("Webhook finalize: no RazorpayPayment found for order {}", razorpayOrderId);
            return;
        }
        if (payment.getPurpose() != RazorpayPurpose.LIBRARY_SIGNUP) return;
        if (payment.getStatus() == RazorpayPaymentStatus.PAID) return; // already done — idempotent

        finalizeAndCreateLibrary(payment, razorpayPaymentId, null, "Finalized via Razorpay webhook");
    }

    /** Shared by the synchronous /verify call and the webhook fallback: creates the library
     *  from the staged payload and marks the payment PAID. Must only be called once signature
     *  trust has already been established by the caller (checkout sig OR webhook sig). */
    private LibraryDTO finalizeAndCreateLibrary(RazorpayPayment payment, String paymentId, String signature, String note) throws Exception {
        LibraryDTO stagedDto = objectMapper.readValue(payment.getStagedPayloadJson(), LibraryDTO.class);
        // Make sure the plan that's actually charged for is the plan that gets applied.
        stagedDto.setLibraryPlanId(payment.getPlanId());

        LibraryDTO created = superAdminService.createLibrary(stagedDto);

        payment.setStatus(RazorpayPaymentStatus.PAID);
        payment.setRazorpayPaymentId(paymentId);
        if (signature != null) payment.setRazorpaySignature(signature);
        payment.setLibraryId(created.getId());
        payment.setNote(note);
        payment.setUpdatedAt(LocalDateTime.now());
        razorpayPaymentRepository.save(payment);

        return created;
    }

    @Override
    @Transactional
    public void cancelSignup(Long paymentRecordId) throws Exception {
        RazorpayPayment payment = razorpayPaymentRepository.findById(paymentRecordId)
                .orElseThrow(() -> new Exception("Payment record not found"));
        if (payment.getStatus() == RazorpayPaymentStatus.CREATED) {
            payment.setStatus(RazorpayPaymentStatus.CANCELLED);
            payment.setUpdatedAt(LocalDateTime.now());
            razorpayPaymentRepository.save(payment);
        }
    }
}
