package com.learningJWT.LearningTemplate.Scheduler;

import com.learningJWT.LearningTemplate.Enum.NotificationType;
import com.learningJWT.LearningTemplate.Enum.SeatStatus;
import com.learningJWT.LearningTemplate.Model.SeatAllocation;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Repository.SeatAllocationRepository;
import com.learningJWT.LearningTemplate.Repository.SeatRepository;
import com.learningJWT.LearningTemplate.Services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatExpiryScheduler {

    private final SeatAllocationRepository seatAllocationRepository;
    private final SeatRepository seatRepository;
    private final NotificationService notificationService;

    /**
     * Daily at 07:00 — auto-releases seats whose endDate has passed.
     * Sends student notification 3 days before expiry.
     */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void processSeatExpiry() {
        log.info("Running seat expiry sweep");
        LocalDate today   = LocalDate.now();
        LocalDate in3days = today.plusDays(3);

        List<SeatAllocation> allocations = seatAllocationRepository.findAll();
        for (SeatAllocation alloc : allocations) {
            if (alloc.getEndDate() == null) continue;
            Student student = alloc.getStudent();

            // Warn 3 days before
            if (!alloc.getEndDate().isBefore(today) && !alloc.getEndDate().isAfter(in3days)) {
                if (student != null && student.getUser() != null) {
                    notificationService.send(student.getUser(), student.getLibrary(),
                            NotificationType.SEAT_EXPIRY,
                            "Seat Expiring Soon",
                            String.format("Your seat %s expires on %s. Contact admin to renew.",
                                    alloc.getSeat() != null ? alloc.getSeat().getSeatName() : "",
                                    alloc.getEndDate()),
                            "/student/fees");
                }
            }

            // Auto-release expired seats
            if (alloc.getEndDate().isBefore(today) && alloc.getSeat() != null
                    && alloc.getSeat().getStatus() == SeatStatus.ALLOCATED) {
                alloc.getSeat().setStatus(SeatStatus.AVAILABLE);
                seatRepository.save(alloc.getSeat());
                log.info("Seat {} auto-released (expired {})",
                        alloc.getSeat().getSeatName(), alloc.getEndDate());
            }
        }
        log.info("Seat expiry sweep complete");
    }
}
