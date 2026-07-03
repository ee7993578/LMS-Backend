package com.learningJWT.LearningTemplate.Scheduler;

import com.learningJWT.LearningTemplate.Repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the notifications table from growing forever: every month, on the 1st at 00:10
 * server time, ALL notifications (every library, every student, read/unread, broadcast
 * and individual alike) are wiped. This is a hard reset by design — see feature request:
 * "1 month ke baad her month ki first date per sare notification delete hone chahiye".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;

    /** Cron: second minute hour day-of-month month day-of-week -> 00:10 on day 1 of every month. */
    @Scheduled(cron = "0 10 0 1 * *")
    @Transactional
    public void wipeAllNotificationsMonthly() {
        long countBefore = notificationRepository.count();
        notificationRepository.deleteAllInBatch();
        log.info("Monthly notification cleanup: deleted {} notifications (all libraries)", countBefore);
    }
}
