package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.OnboardingStatusDTO;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.OnboardingStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/libraryadmin/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingStatusService onboardingStatusService;
    private final UserRepository userRepository;

    private User getLoggedInAdmin() throws Exception {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
        }
        throw new Exception("Not authenticated");
    }

    /** GET /api/libraryadmin/onboarding/status — checklist + progress + recommended next step */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/status")
    public ResponseEntity<OnboardingStatusDTO> getStatus() throws Exception {
        User admin = getLoggedInAdmin();
        return ResponseEntity.ok(onboardingStatusService.getStatus(admin.getLibrary()));
    }

    /** PUT /api/libraryadmin/onboarding/welcome-seen — marks the one-time welcome modal as seen */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping("/welcome-seen")
    public ResponseEntity<?> markWelcomeSeen() throws Exception {
        User admin = getLoggedInAdmin();
        onboardingStatusService.markWelcomeSeen(admin.getLibrary());
        return ResponseEntity.ok(java.util.Map.of("welcomeShown", true));
    }
}
