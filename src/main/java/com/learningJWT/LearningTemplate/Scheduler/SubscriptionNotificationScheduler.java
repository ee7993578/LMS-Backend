package com.learningJWT.LearningTemplate.Scheduler;

import com.learningJWT.LearningTemplate.Enum.NotificationType;
import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.Subscription;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Services.NotificationService;
import com.learningJWT.LearningTemplate.Services.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionNotificationScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final LibraryRepository libraryRepository;
    private final NotificationService notificationService;

    /**
     * Daily at 10:00 — warns library admins whose subscription expires in 7 or 3 days.
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendExpiryWarnings() {
        log.info("Running subscription expiry warning sweep");
        LocalDate today = LocalDate.now();
        List<Subscription> active = subscriptionRepository.findByStatus(Status.ACTIVE);

        for (Subscription sub : active) {
            if (sub.getBillingEnd() == null) continue;
            long daysLeft = ChronoUnit.DAYS.between(today, sub.getBillingEnd());
            Library library = sub.getLibrary();
            if (library == null || library.getAdmin() == null) continue;

            if (daysLeft == 7 || daysLeft == 3 || daysLeft == 1) {
                notificationService.send(
                        library.getAdmin(), library, NotificationType.PLAN_EXPIRY,
                        "Subscription Expiring Soon",
                        String.format("Your '%s' plan expires in %d day(s) on %s. Renew to avoid service interruption.",
                                sub.getPlanName() != null ? sub.getPlanName() : "current",
                                daysLeft, sub.getBillingEnd()),
                        "/admin/settings");
            }
        }
        log.info("Subscription expiry warning sweep complete");
    }

    /**
     * Daily at 10:30 — sends notification to admin when their library is suspended/expired.
     */
    @Scheduled(cron = "0 30 10 * * *")
    public void notifySuspendedLibraries() {
        List<Library> expired = libraryRepository.findByStatus(Status.EXPIRED_READ_ONLY);
        for (Library lib : expired) {
            if (lib.getAdmin() == null) continue;
            notificationService.send(lib.getAdmin(), lib, NotificationType.PLAN_EXPIRY,
                    "Library Access Restricted",
                    "Your subscription has expired. Your library is in read-only mode. Please renew to restore full access.",
                    "/admin/settings");
        }
    }
}
