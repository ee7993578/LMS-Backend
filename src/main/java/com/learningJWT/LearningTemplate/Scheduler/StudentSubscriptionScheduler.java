package com.learningJWT.LearningTemplate.Scheduler;

import com.learningJWT.LearningTemplate.Enum.NotificationType;
import com.learningJWT.LearningTemplate.Enum.SeatStatus;
import com.learningJWT.LearningTemplate.Enum.StudentSubscriptionStatus;
import com.learningJWT.LearningTemplate.Model.Seat;
import com.learningJWT.LearningTemplate.Model.SeatAllocation;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Model.StudentSubscription;
import com.learningJWT.LearningTemplate.Repository.SeatAllocationRepository;
import com.learningJWT.LearningTemplate.Repository.SeatRepository;
import com.learningJWT.LearningTemplate.Repository.StudentSubscriptionRepository;
import com.learningJWT.LearningTemplate.Services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Daily sweep over StudentSubscription rows. Mirrors the existing codebase philosophy
 * ("never auto-hide/deactivate a student") — this only updates the subscription's own status
 * flag and, optionally (library.autoReleaseSeatOnExpiry), releases the seat. It never touches
 * Student.active or blocks login.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StudentSubscriptionScheduler {

    private final StudentSubscriptionRepository subscriptionRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final SeatRepository seatRepository;
    private final com.learningJWT.LearningTemplate.Repository.StudentRepository studentRepository;
    private final NotificationService notificationService;

    private static final int WARNING_DAYS = 3;

    /** Daily at 06:30 — flips expired ACTIVE cycles to EXPIRED, optionally releases seats, and notifies. */
    @Scheduled(cron = "0 30 6 * * *")
    @Transactional
    public void processSubscriptionExpiry() {
        log.info("Running student subscription expiry sweep");
        LocalDate today = LocalDate.now();
        List<StudentSubscription> active = subscriptionRepository.findByStatus(StudentSubscriptionStatus.ACTIVE);

        for (StudentSubscription sub : active) {
            if (sub.getCycleEnd() == null) continue;
            Student student = sub.getStudent();

            long daysLeft = ChronoUnit.DAYS.between(today, sub.getCycleEnd());

            if (sub.getCycleEnd().isBefore(today)) {
                sub.setStatus(StudentSubscriptionStatus.EXPIRED);
                subscriptionRepository.save(sub);

                if (student != null && student.getUser() != null) {
                    notificationService.send(student.getUser(), sub.getLibrary(),
                            NotificationType.PLAN_EXPIRY,
                            "Plan Expired",
                            String.format("Your plan expired on %s. Please contact admin to renew.", sub.getCycleEnd()),
                            "/student/dashboard");
                }

                boolean autoRelease = sub.getLibrary() != null && sub.getLibrary().isAutoReleaseSeatOnExpiry();
                if (autoRelease && student != null) {
                    releaseSeatForStudent(student);
                }
            } else if (daysLeft == WARNING_DAYS || daysLeft == 1) {
                if (student != null && student.getUser() != null) {
                    notificationService.send(student.getUser(), sub.getLibrary(),
                            NotificationType.PLAN_EXPIRY,
                            "Plan Expiring Soon",
                            String.format("Your plan expires on %s (%d day(s) left). Renew soon to avoid interruption.",
                                    sub.getCycleEnd(), daysLeft),
                            "/student/dashboard");
                }
            }
        }
        log.info("Student subscription expiry sweep complete");
    }

    private void releaseSeatForStudent(Student student) {
        List<SeatAllocation> activeAllocs = seatAllocationRepository.findByStudentIdAndActiveTrue(student.getId());
        for (SeatAllocation alloc : activeAllocs) {
            alloc.setActive(false);
            alloc.setDeallocatedAt(java.time.LocalDateTime.now());
            seatAllocationRepository.save(alloc);

            Seat seat = alloc.getSeat();
            if (seat != null) {
                boolean othersActive = seatAllocationRepository.findBySeatIdAndActiveTrue(seat.getId())
                        .stream().anyMatch(a -> !a.getStudent().getId().equals(student.getId()));
                if (!othersActive) {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seatRepository.save(seat);
                }
            }
        }
        student.setSeat(null);
        studentRepository.save(student);
        log.info("Auto-released seat(s) for student {} on plan expiry (library autoReleaseSeatOnExpiry=true)",
                student.getId());
    }
}
