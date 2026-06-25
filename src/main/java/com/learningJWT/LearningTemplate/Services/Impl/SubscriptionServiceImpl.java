package com.learningJWT.LearningTemplate.Services.Impl;
import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Mapper.LibraryMapper;
import com.learningJWT.LearningTemplate.Mapper.SubscriptionMapper;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import com.learningJWT.LearningTemplate.Model.Subscription;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.SubscriptionDTO;
import com.learningJWT.LearningTemplate.Repository.LibraryPlanRepository;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.SubscriptionRepository;
import com.learningJWT.LearningTemplate.Services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
     private final LibraryRepository libraryRepository;
     private final LibraryPlanRepository libraryPlanRepository;
     private final UserRepository userRepository;
     private final StudentRepository studentRepository;

    @Override
    public SubscriptionDTO createSubscription(Long libraryId, Long planId) throws Exception {

        Library library = libraryRepository.findById(libraryId).orElseThrow(
                ()-> new Exception("Library Not found")
        );

        LibraryPlan libraryPlan = libraryPlanRepository.findById(planId).orElseThrow(
                ()-> new Exception("Plan Not found")
        );

        Subscription subscription = new Subscription();
        subscription.setLibrary(library);
        subscription.setLibraryPlan(libraryPlan);
        subscription.setPlanName(libraryPlan.getPlanName());
        subscription.setStatus(Status.ACTIVE);
        subscription.setBufferAllowed(libraryPlan.getBufferStudent());
        subscription.setStudentLimit(libraryPlan.getNoOfStudent());
        subscription.setPricePerMonth(libraryPlan.getPlanPrice());
        subscription.setBillingStart(LocalDate.now());
        subscription.setBillingEnd(LocalDate.now().plusDays(libraryPlan.getNoOfDays()));
        subscription.setBufferExpiryDays(3);
        //Agar hum grace date ko change karna chahenge to kar sakte hain
        subscription.setGraceEndDate(LocalDate.now().plusDays(libraryPlan.getNoOfDays()+7));
         Subscription saved= subscriptionRepository.save(subscription);

        return SubscriptionMapper.todto(saved);
    }

    @Override
    public SubscriptionDTO upgradePlan(Long libraryId, Long newPlanId) throws Exception {
        Library library = libraryRepository.findById(libraryId).orElseThrow(
                () -> new Exception("Library Not found")
        );

        LibraryPlan newPlan = libraryPlanRepository.findById(newPlanId).orElseThrow(
                () -> new Exception("Plan Not found")
        );

        Subscription currentSubscription = subscriptionRepository.findByLibraryId(libraryId);
        if (currentSubscription == null) {
            throw new Exception("No subscription found for this library");
        }

        if (currentSubscription.getLibraryPlan().getPlanId().equals(newPlanId)) {
            throw new Exception("You already have this plan");
        }

        return getSubscriptionDTO(newPlan, currentSubscription);
    }


    @Override
    public SubscriptionDTO renewSubscription(Long libraryId) throws Exception {

        Library library = libraryRepository.findById(libraryId).orElseThrow(
                () -> new Exception("Library Not found")
        );

        Long planId = library.getLibraryPlan().getPlanId();

        LibraryPlan oldPlan = libraryPlanRepository.findById(planId).orElseThrow(
                () -> new Exception("Plan Not found")
        );

        Subscription currentSubscription = subscriptionRepository.findByLibraryId(libraryId);
        if (currentSubscription == null) {
            throw new Exception("No subscription found for this library");
        }

        return getSubscriptionDTO(oldPlan, currentSubscription);
    }

    private SubscriptionDTO getSubscriptionDTO(LibraryPlan oldPlan, Subscription currentSubscription) {
        LocalDate today = LocalDate.now();
        LocalDate oldEnd = currentSubscription.getBillingEnd();

        long remainingDays = 0;
        if (oldEnd != null && oldEnd.isAfter(today)) {
            long diff = java.time.temporal.ChronoUnit.DAYS.between(today, oldEnd);
            remainingDays = Math.max(diff, 0);
        }
        currentSubscription.setLibraryPlan(oldPlan);
        currentSubscription.setPlanName(oldPlan.getPlanName());
        currentSubscription.setStatus(Status.ACTIVE);
        currentSubscription.setBufferAllowed(oldPlan.getBufferStudent());
        currentSubscription.setStudentLimit(oldPlan.getNoOfStudent());
        currentSubscription.setPricePerMonth(oldPlan.getPlanPrice());
        currentSubscription.setBillingStart(today);
        currentSubscription.setBillingEnd(today.plusDays(oldPlan.getNoOfDays() + remainingDays));
        currentSubscription.setBufferExpiryDays(3);
        currentSubscription.setGraceEndDate(today.plusDays(oldPlan.getNoOfDays() + remainingDays+7));
        Subscription saved = subscriptionRepository.save(currentSubscription);
        return SubscriptionMapper.todto(saved);
    }

    @Override
    public boolean isSubscriptionActive(Long libraryId) throws Exception {

        Subscription subscription = subscriptionRepository.findByLibraryId(libraryId);

        if (subscription == null) {
            throw new Exception("Subscription not found for the library");
        }

        LocalDate today = LocalDate.now();
        if (subscription.getBillingEnd() != null &&
                subscription.getBillingEnd().isBefore(today)) {
            return false;
        }
        if (subscription.getStatus() == Status.GRACE) {
            return false;
        }
        return subscription.getStatus() == Status.ACTIVE;
    }


    @Override
    public void handleBufferExceeded(Long libraryId) {

        Subscription subscription = subscriptionRepository.findByLibraryId(libraryId);

        if (subscription == null) {
            throw new RuntimeException("Subscription not found for library");
        }

        int limit = subscription.getStudentLimit();
        int buffer = subscription.getBufferAllowed();
        int maxAllowed = limit + buffer;

        int totalStudents = studentRepository.countByLibraryId(libraryId);
        if (totalStudents > maxAllowed) {
            Library library = subscription.getLibrary();
            library.setStatus(Status.EXCEEDED);
            libraryRepository.save(library);
        }
    }
//    Mid night scheduler to handle Buffer Exceeded
//    @Scheduled(cron = "0 0 0 * * *")
//    public void checkAllLibrariesBuffer() {
//        List<Subscription> all = subscriptionRepository.findAll();
//        for (Subscription s : all) {
//            handleBufferExceeded(s.getLibrary().getId());
//        }
//    }


    @Override
    public void deleteExpiredBufferStudents() {
        // Intentionally a no-op. Per product requirement, student data is NEVER deleted when a
        // library exceeds its plan limit or its grace period lapses — only NEW student creation
        // is blocked (see LibraryLifecycleService#canRegisterNewStudent). Existing students,
        // including those created during a grace window, remain visible and usable forever.
    }

    @Override
    public void deactivateExpiredSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Subscription> activeSubs = subscriptionRepository.findByStatus(Status.ACTIVE);
        for (Subscription sub : activeSubs) {
            LocalDate billingEnd = sub.getBillingEnd();
            if (billingEnd != null && billingEnd.isBefore(today)) {
                sub.setStatus(Status.EXPIRED);
                sub.setGraceEndDate(billingEnd.plusDays(7));
                sub.setStatusChangedAt(java.time.LocalDateTime.now());
                Library lib = sub.getLibrary();
                // EXPIRED_READ_ONLY: subscription lapsed, library gets a 7-day read-only window
                // (see LibraryLifecycleScheduler for the full ACTIVE -> EXPIRED_READ_ONLY -> INACTIVE -> DELETED flow).
                lib.setStatus(Status.EXPIRED_READ_ONLY);
                lib.setStatusChangedAt(java.time.LocalDateTime.now());
                libraryRepository.save(lib);
                subscriptionRepository.save(sub);
            }
        }
    }

    /**
     * NOTE: This method is intentionally NOT used by the scheduler anymore (see
     * LibraryLifecycleScheduler, which implements the full read-only -> inactive -> soft-delete
     * flow without ever hard-deleting rows). It is kept only so the existing
     * SubscriptionService interface and any direct callers keep compiling, and it now performs
     * a safe, reversible soft-delete instead of destroying library/admin/student data.
     */
    @Override
    public void deleteLibrariesAfterGracePeriod() {

        LocalDate today = LocalDate.now();

        List<Subscription> graceSubscriptions =
                subscriptionRepository.findByStatus(Status.GRACE);

        for (Subscription subscription : graceSubscriptions) {

            LocalDate graceEnd = subscription.getGraceEndDate();

            if (graceEnd == null || !graceEnd.isBefore(today)) {
                continue;
            }

            // Grace period over -> soft-delete only. Library, admin, and student rows are
            // preserved; we just flip status so the library stops being usable.
            Library library = subscription.getLibrary();
            library.setStatus(Status.INACTIVE);
            library.setStatusChangedAt(java.time.LocalDateTime.now());
            libraryRepository.save(library);

            subscription.setStatus(Status.INACTIVE);
            subscription.setStatusChangedAt(java.time.LocalDateTime.now());
            subscriptionRepository.save(subscription);
        }
    }


    @Override
    public List<LibraryDTO> getAllLibraryBySubscriptionStatus(Status status) {

        return subscriptionRepository.findByStatus(status)
                .stream()
                .map(sub -> LibraryMapper.toDTO(sub.getLibrary()))
                .toList();
    }


    @Override
    public SubscriptionDTO getSubscriptionByLibraryId(Long libraryId) throws Exception {

        Subscription subscription = subscriptionRepository.findByLibraryId(libraryId);

        if (subscription == null) {
            throw new Exception("Subscription not found for the library");
        }

        return SubscriptionMapper.todto(subscription);
    }

}
