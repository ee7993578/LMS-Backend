package com.learningJWT.LearningTemplate.Scheduler;

import com.learningJWT.LearningTemplate.Enum.FeeStatus;
import com.learningJWT.LearningTemplate.Enum.NotificationType;
import com.learningJWT.LearningTemplate.Model.Fee;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Repository.FeeRepository;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
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
public class FeeScheduler {

    private final FeeRepository feeRepository;
    private final LibraryRepository libraryRepository;
    private final NotificationService notificationService;

    /**
     * Runs daily at 09:00 — sends fee-due notifications for upcoming dues (3 days ahead)
     * and fee-overdue notifications for already-past dues.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendFeeDueNotifications() {
        log.info("Running fee due notification sweep");
        LocalDate today    = LocalDate.now();
        LocalDate in3days  = today.plusDays(3);

        List<Library> libraries = libraryRepository.findAll();
        for (Library library : libraries) {
            List<Fee> fees = feeRepository.findByLibraryId(library.getId());
            for (Fee fee : fees) {
                if (fee.getFeeStatus() == FeeStatus.PAID) continue;
                Student student = fee.getStudent();
                if (student == null || student.getUser() == null) continue;

                LocalDate due = fee.getDueDate();
                if (due == null) continue;

                // Upcoming — due in <= 3 days
                if (!due.isBefore(today) && !due.isAfter(in3days)) {
                    notificationService.send(
                            student.getUser(), library, NotificationType.FEE_DUE,
                            "Fee Due Soon",
                            String.format("Your fee of ₹%.0f is due on %s. Please pay to avoid late charges.",
                                    fee.getBalance(), due),
                            "/student/fees");
                }

                // Overdue
                if (due.isBefore(today) && fee.getFeeStatus() != FeeStatus.PAID) {
                    long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(due, today);
                    notificationService.send(
                            student.getUser(), library, NotificationType.FEE_OVERDUE,
                            "Fee Overdue",
                            String.format("Your fee of ₹%.0f is %d day(s) overdue. Late charges may apply.",
                                    fee.getBalance(), daysOverdue),
                            "/student/fees");
                }
            }
        }
        log.info("Fee due notification sweep complete");
    }

    /**
     * Runs on 1st of every month at 00:30 — auto-applies late fee to
     * any UNPAID or PARTIAL fees from the previous month.
     */
    @Scheduled(cron = "0 30 0 1 * *")
    @Transactional
    public void autoApplyLateFees() {
        log.info("Running auto late-fee sweep");
        LocalDate today = LocalDate.now();
        List<Library> libraries = libraryRepository.findAll();

        for (Library library : libraries) {
            List<Fee> fees = feeRepository.findByLibraryId(library.getId());
            for (Fee fee : fees) {
                if (fee.getFeeStatus() == FeeStatus.PAID) continue;
                if (fee.getDueDate() == null || !fee.getDueDate().isBefore(today)) continue;
                // Only apply if no late fee already set
                if (fee.getLateFee() > 0) continue;

                double lateFee = Math.min(50.0, fee.getBalance() * 0.05); // 5% or max ₹50
                fee.setLateFee(lateFee);
                fee.setBalance(fee.getBalance() + lateFee);
                feeRepository.save(fee);
                log.debug("Late fee ₹{} applied to fee {} for student {}",
                        lateFee, fee.getFeeId(), fee.getStudent() != null ? fee.getStudent().getId() : "?");
            }
        }
        log.info("Auto late-fee sweep complete");
    }

    /**
     * Runs daily at 08:00 — sends admin digest of pending dues summary.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendAdminPendingDuesDigest() {
        log.info("Running admin pending dues digest");
        List<Library> libraries = libraryRepository.findAll();
        for (Library library : libraries) {
            if (library.getAdmin() == null) continue;
            List<Fee> pending = feeRepository.findPendingFeesByLibrary(library.getId());
            if (pending.isEmpty()) continue;

            long overdueCount = pending.stream()
                    .filter(f -> f.getDueDate() != null && f.getDueDate().isBefore(LocalDate.now()))
                    .count();
            double totalPending = pending.stream().mapToDouble(Fee::getBalance).sum();

            if (overdueCount > 0) {
                notificationService.send(
                        library.getAdmin(), library, NotificationType.FEE_OVERDUE,
                        "Pending Dues Digest",
                        String.format("%d students have overdue fees. Total pending: ₹%.0f",
                                overdueCount, totalPending),
                        "/admin/reports");
            }
        }
        log.info("Admin pending dues digest done");
    }
}
