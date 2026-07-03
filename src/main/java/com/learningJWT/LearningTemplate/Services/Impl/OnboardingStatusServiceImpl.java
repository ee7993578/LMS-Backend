package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Paylod.DTO.OnboardingStatusDTO;
import com.learningJWT.LearningTemplate.Repository.*;
import com.learningJWT.LearningTemplate.Services.OnboardingStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the "getting started" checklist for a library. Deliberately stateless/derived —
 * every flag is read live from the actual data (a plan exists, a seat exists, etc.) rather than
 * a separately-tracked "did the admin click this" flag, so it can never drift out of sync with
 * reality. QR / attendance setup is intentionally NOT a step here — it's provisioned
 * automatically the moment the library is created (see SuperAdminServiceImpl#createLibrary),
 * so the admin never has to think about it.
 *
 * To add a future onboarding step: add one entry to buildStepDefinitions() below.
 */
@Service
@RequiredArgsConstructor
public class OnboardingStatusServiceImpl implements OnboardingStatusService {

    private final PlanRepository planRepository;
    private final SeatRepository seatRepository;
    private final StudentRepository studentRepository;
    private final PaymentSettingsRepository paymentSettingsRepository;
    private final LibraryRepository libraryRepository;

    private interface Check {
        boolean isDone(Library library);
    }

    private OnboardingStatusDTO.Step step(String key, String title, String description,
                                           String estimatedTime, String actionLabel,
                                           String actionRoute, boolean completed) {
        return OnboardingStatusDTO.Step.builder()
                .key(key).title(title).description(description).estimatedTime(estimatedTime)
                .actionLabel(actionLabel).actionRoute(actionRoute).completed(completed)
                .build();
    }

    @Override
    public OnboardingStatusDTO getStatus(Library library) {
        Long libraryId = library.getId();

        boolean plansCreated = !planRepository.findByLibraryId(libraryId).isEmpty();
        boolean seatsCreated = !seatRepository.findByLibraryId(libraryId).isEmpty();
        boolean paymentConfigured = paymentSettingsRepository.findByLibraryId(libraryId).isPresent();
        boolean selfRegistrationEnabled = library.isRegistrationEnabled();
        boolean firstStudentAdded = studentRepository.countByLibraryId(libraryId) > 0;

        List<OnboardingStatusDTO.Step> steps = new ArrayList<>();
        steps.add(step(
                "PLANS", "Create Student Plans",
                "Student Plans define fees and validity. Students cannot be added until at least one plan exists.",
                "2 Minutes", "Create Plan", "/admin/plans", plansCreated));
        steps.add(step(
                "SEATS", "Create Seats",
                "Seats represent physical seating positions inside your library. Students will later be assigned to these seats.",
                "2 Minutes", "Create Seats", "/admin/seats", seatsCreated));
        steps.add(step(
                "PAYMENT", "Configure Payment",
                "Configure your payment settings so students can make payments and records remain organized.",
                "1 Minute", "Configure Payment", "/admin/payment", paymentConfigured));
        steps.add(step(
                "SELF_REGISTRATION", "Enable Self Registration",
                "Allow students to register themselves using the registration page and automatically join your library. You can enable or disable this anytime.",
                "30 Seconds", "Enable", "/admin/settings", selfRegistrationEnabled));
        steps.add(step(
                "FIRST_STUDENT", "Add Your First Student",
                "Add your first student manually to start using the system immediately. Later you can use Self Registration for additional students.",
                "1 Minute", "Add Student", "/admin/students", firstStudentAdded));

        long completed = steps.stream().filter(OnboardingStatusDTO.Step::isCompleted).count();
        int total = steps.size();
        int percentage = total == 0 ? 100 : (int) Math.round((completed * 100.0) / total);
        boolean allCompleted = completed == total;

        OnboardingStatusDTO.Step recommended = allCompleted ? null
                : steps.stream().filter(s -> !s.isCompleted()).findFirst().orElse(null);

        return OnboardingStatusDTO.builder()
                .welcomeShown(library.isOnboardingWelcomeShown())
                .steps(steps)
                .completedCount((int) completed)
                .totalCount(total)
                .percentage(percentage)
                .allCompleted(allCompleted)
                .recommendedNextStep(recommended)
                .build();
    }

    @Override
    public void markWelcomeSeen(Library library) {
        if (!library.isOnboardingWelcomeShown()) {
            library.setOnboardingWelcomeShown(true);
            libraryRepository.save(library);
        }
    }
}
