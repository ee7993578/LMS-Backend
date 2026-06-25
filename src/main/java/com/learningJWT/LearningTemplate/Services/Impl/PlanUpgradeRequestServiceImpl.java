package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Enum.PlanRequestStatus;
import com.learningJWT.LearningTemplate.Mapper.PlanUpgradeRequestMapper;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import com.learningJWT.LearningTemplate.Model.PlanUpgradeRequest;
import com.learningJWT.LearningTemplate.Model.Subscription;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.PlanUpgradeRequestDTO;
import com.learningJWT.LearningTemplate.Repository.LibraryPlanRepository;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Repository.PlanUpgradeRequestRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.PlanUpgradeRequestService;
import com.learningJWT.LearningTemplate.Services.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

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

        Library library = request.getLibrary();
        LibraryPlan newPlan = request.getRequestedPlan();

        // Actually apply the plan change to the library + its subscription record.
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

        request.setStatus(PlanRequestStatus.APPROVED);
        request.setResolutionNote(resolutionNote);
        request.setResolvedAt(LocalDateTime.now());
        requestRepository.save(request);

        return PlanUpgradeRequestMapper.toDTO(request);
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
}
