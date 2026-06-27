package com.learningJWT.LearningTemplate.Scheduler;

import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.Subscription;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Services.LibraryLifecycleService;
import com.learningJWT.LearningTemplate.Services.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implements the automated subscription lifecycle flow end-to-end:
 *
 *   ACTIVE -> (on billingEnd expiry) EXPIRED_READ_ONLY (7 days)
 *   -> INACTIVE
 *   -> after 90 days of INACTIVE -> DELETED (soft delete; data is preserved)
 *
 * Every transition here is a status flip only — no row is ever hard-deleted, so library,
 * admin user, and student records remain intact even once a library reaches DELETED.
 *
 * NOTE: registration no longer puts a library into a TRIAL phase — every new library is
 * assigned the default (lowest planOrder) plan and started directly as ACTIVE (see
 * LibraryLifecycleService#assignDefaultPlanAndActivate). The TRIAL / TRIAL_READ_ONLY
 * transition methods below are kept only so SuperAdmin can still manually move a library
 * into those states if ever needed; they are NOT part of the automated daily sweep.
 */
@Component
@RequiredArgsConstructor
public class LibraryLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(LibraryLifecycleScheduler.class);

    private final LibraryRepository libraryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LibraryLifecycleService lifecycleService;

    /** Runs once a day at 00:05 server time. Drives the subscription status transitions. */
    @Scheduled(cron = "0 5 0 * * *")
    public void runDailyLifecycleSweep() {
        log.info("Running daily library lifecycle sweep");
        expireSubscriptionsToReadOnly();
        expireSubscriptionReadOnlyToInactive();
        deleteLongInactiveSubscriptionLibraries();
        log.info("Daily library lifecycle sweep complete");
    }

    // ===================== TRIAL FLOW (dormant — manual SuperAdmin use only) =====================

    /** TRIAL -> TRIAL_READ_ONLY once trialEndDate has passed. */
    public void expireTrials() {
        List<Library> trials = libraryRepository.findByStatus(Status.TRIAL);
        LocalDateTime now = LocalDateTime.now();
        for (Library library : trials) {
            if (library.getTrialEndDate() != null && now.isAfter(library.getTrialEndDate())) {
                lifecycleService.transitionTo(library, Status.TRIAL_READ_ONLY);
                libraryRepository.save(library);
                log.info("Library {} moved TRIAL -> TRIAL_READ_ONLY", library.getId());
            }
        }
    }

    /** TRIAL_READ_ONLY -> INACTIVE after 7 more days. */
    public void expireTrialReadOnlyToInactive() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(LibraryLifecycleService.TRIAL_READ_ONLY_DAYS);
        List<Library> expired = libraryRepository.findByStatusAndStatusChangedAtBefore(Status.TRIAL_READ_ONLY, cutoff);
        for (Library library : expired) {
            lifecycleService.transitionTo(library, Status.INACTIVE);
            libraryRepository.save(library);
            log.info("Library {} moved TRIAL_READ_ONLY -> INACTIVE", library.getId());
        }
    }

    /** INACTIVE (that came from the trial path, i.e. never had a paid subscription) -> DELETED
     *  after 30 days. We distinguish "trial path" vs "subscription path" by checking whether a
     *  Subscription with a billingStart exists; libraries that never paid follow the 30-day rule,
     *  libraries that did pay and lapsed follow the 90-day rule below. */
    public void deleteLongInactiveTrialLibraries() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(LibraryLifecycleService.TRIAL_INACTIVE_TO_DELETE_DAYS);
        List<Library> inactive = libraryRepository.findByStatusAndStatusChangedAtBefore(Status.INACTIVE, cutoff);
        for (Library library : inactive) {
            Subscription subscription = subscriptionRepository.findByLibraryId(library.getId());
            boolean neverPaid = subscription == null || subscription.getBillingStart() == null;
            if (neverPaid) {
                lifecycleService.transitionTo(library, Status.DELETED);
                libraryRepository.save(library);
                log.info("Library {} soft-deleted after 30 days INACTIVE (trial path)", library.getId());
            }
        }
    }

    // ===================== SUBSCRIPTION FLOW =====================

    /** ACTIVE -> EXPIRED_READ_ONLY once the subscription's billingEnd has passed. */
    public void expireSubscriptionsToReadOnly() {
        List<Subscription> activeSubs = subscriptionRepository.findByStatus(Status.ACTIVE);
        LocalDate today = LocalDate.now();
        for (Subscription sub : activeSubs) {
            if (sub.getBillingEnd() != null && sub.getBillingEnd().isBefore(today)) {
                sub.setStatus(Status.EXPIRED);
                sub.setStatusChangedAt(LocalDateTime.now());
                sub.setGraceEndDate(sub.getBillingEnd().plusDays(LibraryLifecycleService.SUBSCRIPTION_EXPIRED_READ_ONLY_DAYS));
                subscriptionRepository.save(sub);

                Library library = sub.getLibrary();
                lifecycleService.transitionTo(library, Status.EXPIRED_READ_ONLY);
                libraryRepository.save(library);
                log.info("Library {} moved ACTIVE -> EXPIRED_READ_ONLY", library.getId());
            }
        }
    }

    /** EXPIRED_READ_ONLY -> INACTIVE after 7 more days. */
    public void expireSubscriptionReadOnlyToInactive() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(LibraryLifecycleService.SUBSCRIPTION_EXPIRED_READ_ONLY_DAYS);
        List<Library> expired = libraryRepository.findByStatusAndStatusChangedAtBefore(Status.EXPIRED_READ_ONLY, cutoff);
        for (Library library : expired) {
            lifecycleService.transitionTo(library, Status.INACTIVE);
            libraryRepository.save(library);

            Subscription sub = subscriptionRepository.findByLibraryId(library.getId());
            if (sub != null) {
                sub.setStatus(Status.INACTIVE);
                sub.setStatusChangedAt(LocalDateTime.now());
                subscriptionRepository.save(sub);
            }
            log.info("Library {} moved EXPIRED_READ_ONLY -> INACTIVE", library.getId());
        }
    }

    /** INACTIVE (subscription path: library DID have a paid subscription that lapsed) -> DELETED
     *  after 90 days. */
    public void deleteLongInactiveSubscriptionLibraries() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(LibraryLifecycleService.SUBSCRIPTION_INACTIVE_TO_DELETE_DAYS);
        List<Library> inactive = libraryRepository.findByStatusAndStatusChangedAtBefore(Status.INACTIVE, cutoff);
        for (Library library : inactive) {
            Subscription subscription = subscriptionRepository.findByLibraryId(library.getId());
            boolean everPaid = subscription != null && subscription.getBillingStart() != null;
            if (everPaid) {
                lifecycleService.transitionTo(library, Status.DELETED);
                libraryRepository.save(library);
                log.info("Library {} soft-deleted after 90 days INACTIVE (subscription path)", library.getId());
            }
        }
    }
}
// NOTE: Notification hooks are added at the service layer (LibraryLifecycleService)
