package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Enum.PlanUsageStatus;
import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Enum.UserRole;
import com.learningJWT.LearningTemplate.Mapper.LibraryMapper;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import com.learningJWT.LearningTemplate.Model.Subscription;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryUsageDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.SuperAdminDashboardDTO;
import com.learningJWT.LearningTemplate.Repository.LibraryPlanRepository;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.LibraryLifecycleService;
import com.learningJWT.LearningTemplate.Services.SubscriptionRepository;
import com.learningJWT.LearningTemplate.Services.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final LibraryRepository libraryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LibraryPlanRepository libraryPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StudentRepository studentRepository;
    private final LibraryLifecycleService lifecycleService;

    @Override
    public LibraryDTO createLibrary(LibraryDTO dto) throws Exception {

        if (dto.getAdminUsername() == null || dto.getAdminUsername().isBlank()) {
            throw new Exception("Admin username is required!");
        }

        if (userRepository.existsByUsername(dto.getAdminUsername())) {
            throw new Exception("Admin username already exists!");
        }

        Library library = LibraryMapper.toEntity(dto);
        library.setCreatedAt(LocalDateTime.now());
        library.setUpdatedAt(LocalDateTime.now());

        // Auto-generate unique library code for self-registration
        if (library.getLibraryCode() == null) {
            String code;
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            java.util.Random rnd = new java.util.Random();
            do {
                StringBuilder sb = new StringBuilder("LIB-");
                for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
                code = sb.toString();
            } while (libraryRepository.findByLibraryCode(code).isPresent());
            library.setLibraryCode(code);
        }
        library.setRegistrationEnabled(true);
        library.setRequireAdminApproval(true);

        // If the caller (e.g. SuperAdmin manually creating a library) explicitly chose a plan,
        // honor that. Otherwise — and this is the normal public self-registration path — fall
        // back to the default (lowest planOrder, i.e. the free/starter) plan automatically.
        LibraryPlan plan = null;
        if (dto.getLibraryPlanId() != null) {
            plan = libraryPlanRepository.findById(dto.getLibraryPlanId())
                    .orElseThrow(() -> new Exception("Library plan not found"));
            library.setLibraryPlan(plan);
            lifecycleService.transitionTo(library, Status.ACTIVE);
        } else {
            // assignDefaultPlanAndActivate sets library.libraryPlan + status=ACTIVE in one step.
            plan = lifecycleService.assignDefaultPlanAndActivate(library);
        }

        libraryRepository.save(library);

        User admin = new User();
        admin.setUsername(dto.getAdminUsername());
        admin.setFullName(dto.getAdminFullName());
        admin.setPhone(dto.getAdminPhone());
        admin.setPassword(passwordEncoder.encode(dto.getAdminPassword()));
        admin.setRole(UserRole.ROLE_LIBRARY_ADMIN);
        admin.setLibrary(library);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());

        userRepository.save(admin);

        library.setAdmin(admin);
        libraryRepository.save(library);

        // Library goes live as ACTIVE immediately, so the Subscription record reflects a real,
        // already-running billing cycle from day one — no PENDING/placeholder state.
        int billingDays = (plan != null && plan.getNoOfDays() != null) ? plan.getNoOfDays() : 30;
        LocalDate billingStart = LocalDate.now();
        Subscription subscription = Subscription.builder()
                .library(library)
                .libraryPlan(plan)
                .planName(plan != null ? plan.getPlanName() : null)
                .studentLimit(plan != null ? plan.getNoOfStudent() : null)
                .bufferAllowed(plan != null ? plan.getBufferStudent() : null)
                .bufferExpiryDays(plan != null && plan.getGracePeriodDays() != null ? plan.getGracePeriodDays() : 3)
                .pricePerMonth(plan != null ? plan.getPlanPrice() : null)
                .billingStart(billingStart)
                .billingEnd(billingStart.plusDays(billingDays))
                .status(Status.ACTIVE)
                .statusChangedAt(LocalDateTime.now())
                .build();
        subscriptionRepository.save(subscription);

        return LibraryMapper.toDTO(library);
    }

    @Override
    public LibraryDTO updateLibrary(Long libraryId, LibraryDTO dto) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));

        library.setName(dto.getName());
        library.setAddress(dto.getAddress());
        library.setEmail(dto.getEmail());
        library.setPhone(dto.getPhone());
        library.setWebsite(dto.getWebsite());
        if (dto.getStatus() != null) {
            // Manual status edits from the generic update endpoint are still allowed for
            // SuperAdmin (e.g. fixing a mistake), but stamp statusChangedAt so the scheduler's
            // countdowns stay correct relative to the change.
            if (dto.getStatus() != library.getStatus()) {
                lifecycleService.transitionTo(library, dto.getStatus());
            }
        }
        library.setUpdatedAt(LocalDateTime.now());

        if (dto.getLibraryPlanId() != null) {
            LibraryPlan plan = libraryPlanRepository.findById(dto.getLibraryPlanId())
                    .orElseThrow(() -> new Exception("Library plan not found"));
            library.setLibraryPlan(plan);
        }

        User admin = library.getAdmin();
        if (admin != null) {
            if (dto.getAdminFullName() != null) admin.setFullName(dto.getAdminFullName());
            if (dto.getAdminPhone() != null) admin.setPhone(dto.getAdminPhone());
            if (dto.getAdminPassword() != null && !dto.getAdminPassword().isEmpty()) {
                admin.setPassword(passwordEncoder.encode(dto.getAdminPassword()));
            }
            admin.setUpdatedAt(LocalDateTime.now());
            userRepository.save(admin);
        }

        libraryRepository.save(library);
        return LibraryMapper.toDTO(library);
    }

    /**
     * Soft-delete only. Library, its admin user, and ALL student data are preserved in the
     * database — we only flip the status to DELETED so the library disappears from active
     * operation (login blocked, excluded from active lists). This matches the requirement
     * that student data must never be deleted, and keeps the action reversible by SuperAdmin
     * (see restoreLibrary). Use this instead of hard-deleting rows.
     */
    @Override
    public void deleteLibrary(Long libraryId) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));
        lifecycleService.softDelete(library);
        libraryRepository.save(library);
    }

    @Override
    public List<LibraryDTO> getAllLibraries() {
        List<Library> libraries = libraryRepository.findAll();
        return libraries.stream().map(LibraryMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public LibraryDTO findById(Long libraryId) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));
        return LibraryMapper.toDTO(library);
    }

    @Override
    public List<LibraryDTO> findByStatus(String status) throws Exception {
        List<Library> libraries = libraryRepository.findByStatus(Status.valueOf(status));
        return libraries.stream().map(LibraryMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public LibraryDTO changeLibraryStatus(Long libraryId, String status) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));
        lifecycleService.transitionTo(library, Status.valueOf(status));
        libraryRepository.save(library);
        return LibraryMapper.toDTO(library);
    }

    @Override
    public LibraryDTO assignPlanToLibrary(Long libraryId, Long planId) {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));
        LibraryPlan plan = libraryPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        library.setLibraryPlan(plan);
        Library updated = libraryRepository.save(library);

        Subscription subscription = subscriptionRepository.findByLibraryId(libraryId);
        if (subscription != null) {
            subscription.setLibraryPlan(plan);
            subscription.setPlanName(plan.getPlanName());
            subscription.setStudentLimit(plan.getNoOfStudent());
            subscription.setBufferAllowed(plan.getBufferStudent());
            subscription.setPricePerMonth(plan.getPlanPrice());
            subscriptionRepository.save(subscription);
        }
        return LibraryMapper.toDTO(updated);
    }

    // ===================== Dashboard =====================

    @Override
    public SuperAdminDashboardDTO getDashboardStats() throws Exception {
        long total = libraryRepository.count();
        long trial = libraryRepository.countByStatus(Status.TRIAL) + libraryRepository.countByStatus(Status.TRIAL_READ_ONLY);
        long active = libraryRepository.countByStatus(Status.ACTIVE);
        long expired = libraryRepository.countByStatus(Status.EXPIRED_READ_ONLY);
        long inactive = libraryRepository.countByStatus(Status.INACTIVE);
        long deleted = libraryRepository.countByStatus(Status.DELETED);
        long pending = libraryRepository.countByStatus(Status.PENDING);

        long totalStudents = studentRepository.count();

        List<Subscription> activeSubs = subscriptionRepository.findByStatus(Status.ACTIVE);
        double mrr = activeSubs.stream()
                .filter(s -> s.getPricePerMonth() != null)
                .mapToDouble(Subscription::getPricePerMonth)
                .sum();

        // Best-effort lifetime revenue estimate: for every subscription that has ever had a
        // billingStart, count one full plan price per elapsed billing period since start.
        double totalRevenue = subscriptionRepository.findAll().stream()
                .filter(s -> s.getBillingStart() != null && s.getPricePerMonth() != null)
                .mapToDouble(s -> {
                    LocalDate start = s.getBillingStart();
                    LocalDate end = s.getBillingEnd() != null && s.getBillingEnd().isBefore(LocalDate.now())
                            ? s.getBillingEnd() : LocalDate.now();
                    long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
                    long periods = Math.max(1, days / 30);
                    return periods * s.getPricePerMonth();
                })
                .sum();

        return SuperAdminDashboardDTO.builder()
                .totalLibraries(total)
                .trialLibraries(trial)
                .activeLibraries(active)
                .expiredLibraries(expired)
                .inactiveLibraries(inactive)
                .deletedLibraries(deleted)
                .pendingLibraries(pending)
                .totalStudents(totalStudents)
                .monthlyRecurringRevenue(mrr)
                .totalRevenueCollected(totalRevenue)
                .build();
    }

    // ===================== Lifecycle actions =====================

    @Override
    public LibraryDTO activateLibrary(Long libraryId) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));
        lifecycleService.activate(library);
        libraryRepository.save(library);

        Subscription subscription = subscriptionRepository.findByLibraryId(libraryId);
        if (subscription != null) {
            subscription.setStatus(Status.ACTIVE);
            subscription.setStatusChangedAt(LocalDateTime.now());
            if (subscription.getBillingStart() == null) {
                subscription.setBillingStart(LocalDate.now());
                int days = library.getLibraryPlan() != null && library.getLibraryPlan().getNoOfDays() != null
                        ? library.getLibraryPlan().getNoOfDays() : 30;
                subscription.setBillingEnd(LocalDate.now().plusDays(days));
            }
            subscriptionRepository.save(subscription);
        }
        return LibraryMapper.toDTO(library);
    }

    @Override
    public LibraryDTO suspendLibrary(Long libraryId) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));
        lifecycleService.suspend(library);
        libraryRepository.save(library);
        return LibraryMapper.toDTO(library);
    }

    @Override
    public LibraryDTO restoreLibrary(Long libraryId) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));
        if (library.getStatus() != Status.DELETED && library.getStatus() != Status.INACTIVE) {
            throw new Exception("Only INACTIVE or DELETED libraries can be restored");
        }
        // Restoring puts the library back into ACTIVE if it has a current paid subscription
        // window, otherwise back into TRIAL_READ_ONLY/EXPIRED_READ_ONLY judgment is left to
        // SuperAdmin via a follow-up renew/activate call. Default to ACTIVE + fresh billing
        // only if SuperAdmin explicitly renews; here we just unblock login as read-only.
        Subscription subscription = subscriptionRepository.findByLibraryId(libraryId);
        boolean hasValidBilling = subscription != null && subscription.getBillingEnd() != null
                && !subscription.getBillingEnd().isBefore(LocalDate.now());
        lifecycleService.transitionTo(library, hasValidBilling ? Status.ACTIVE : Status.EXPIRED_READ_ONLY);
        libraryRepository.save(library);
        return LibraryMapper.toDTO(library);
    }

    @Override
    public LibraryDTO renewSubscription(Long libraryId, Integer days) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));

        Subscription subscription = subscriptionRepository.findByLibraryId(libraryId);
        if (subscription == null) {
            throw new Exception("No subscription record found for this library");
        }

        int renewalDays = days != null ? days
                : (library.getLibraryPlan() != null && library.getLibraryPlan().getNoOfDays() != null
                    ? library.getLibraryPlan().getNoOfDays() : 30);

        LocalDate base = (subscription.getBillingEnd() != null && subscription.getBillingEnd().isAfter(LocalDate.now()))
                ? subscription.getBillingEnd() : LocalDate.now();
        subscription.setBillingStart(subscription.getBillingStart() != null ? subscription.getBillingStart() : LocalDate.now());
        subscription.setBillingEnd(base.plusDays(renewalDays));
        subscription.setStatus(Status.ACTIVE);
        subscription.setStatusChangedAt(LocalDateTime.now());
        subscription.setGraceEndDate(null);
        subscriptionRepository.save(subscription);

        lifecycleService.transitionTo(library, Status.ACTIVE);
        libraryRepository.save(library);

        return LibraryMapper.toDTO(library);
    }

    @Override
    public LibraryDTO upgradePlan(Long libraryId, Long newPlanId) throws Exception {
        return changePlan(libraryId, newPlanId);
    }

    @Override
    public LibraryDTO downgradePlan(Long libraryId, Long newPlanId) throws Exception {
        // Downgrade rule: existing students are NEVER removed even if the new plan's limit
        // is lower than the current student count. The library will simply show as already
        // in (or past) its grace period under the new, smaller limit — new registrations
        // get blocked by the usual plan-limit check until the count comes back down naturally
        // (e.g. students leaving) or SuperAdmin assigns a bigger plan again.
        return changePlan(libraryId, newPlanId);
    }

    private LibraryDTO changePlan(Long libraryId, Long newPlanId) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));
        LibraryPlan newPlan = libraryPlanRepository.findById(newPlanId)
                .orElseThrow(() -> new Exception("Plan not found"));

        library.setLibraryPlan(newPlan);
        // Changing plans changes the grace math, so reset the grace timer; canRegisterNewStudent
        // will re-evaluate and restart it if the library is still over the new limit.
        library.setGracePeriodStartedAt(null);
        libraryRepository.save(library);

        Subscription subscription = subscriptionRepository.findByLibraryId(libraryId);
        if (subscription != null) {
            subscription.setLibraryPlan(newPlan);
            subscription.setPlanName(newPlan.getPlanName());
            subscription.setStudentLimit(newPlan.getNoOfStudent());
            subscription.setBufferAllowed(newPlan.getBufferStudent());
            subscription.setBufferExpiryDays(newPlan.getGracePeriodDays() != null ? newPlan.getGracePeriodDays() : 3);
            subscription.setPricePerMonth(newPlan.getPlanPrice());
            subscriptionRepository.save(subscription);
        }

        return LibraryMapper.toDTO(library);
    }

    // ===================== Usage =====================

    @Override
    public LibraryUsageDTO getLibraryUsage(Long libraryId) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));
        return buildUsageDTO(library);
    }

    @Override
    public List<LibraryUsageDTO> getAllLibraryUsage() throws Exception {
        return libraryRepository.findByStatusNot(Status.DELETED).stream()
                .map(this::buildUsageDTOQuiet)
                .collect(Collectors.toList());
    }

    private LibraryUsageDTO buildUsageDTOQuiet(Library library) {
        try {
            return buildUsageDTO(library);
        } catch (Exception e) {
            return LibraryUsageDTO.builder().libraryId(library.getId()).libraryName(library.getName()).build();
        }
    }

    private LibraryUsageDTO buildUsageDTO(Library library) throws Exception {
        long count = lifecycleService.currentStudentCount(library.getId());
        LibraryPlan plan = library.getLibraryPlan();
        PlanUsageStatus usageStatus = lifecycleService.evaluatePlanUsage(library);

        return LibraryUsageDTO.builder()
                .libraryId(library.getId())
                .libraryName(library.getName())
                .planName(plan != null ? plan.getPlanName() : null)
                .planLimit(plan != null ? plan.getNoOfStudent() : null)
                .graceLimit(plan != null && plan.getNoOfStudent() != null
                        ? plan.getNoOfStudent() + (plan.getBufferStudent() != null ? plan.getBufferStudent() : 0)
                        : null)
                .currentStudentCount(count)
                .inGracePeriod(usageStatus == PlanUsageStatus.IN_GRACE || usageStatus == PlanUsageStatus.GRACE_EXCEEDED)
                .graceDaysRemaining(lifecycleService.graceDaysRemaining(library))
                .graceExceeded(usageStatus == PlanUsageStatus.GRACE_EXCEEDED)
                .status(library.getStatus())
                .daysRemainingInCurrentPhase(LibraryMapper.toDTO(library).getDaysRemainingInCurrentPhase())
                .build();
    }
}
