package com.learningJWT.LearningTemplate.Migration;

import com.learningJWT.LearningTemplate.Enum.StudentSubscriptionChangeType;
import com.learningJWT.LearningTemplate.Enum.StudentSubscriptionStatus;
import com.learningJWT.LearningTemplate.Model.Plan;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Model.StudentSubscription;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Repository.StudentSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One-time data migration: backfills a StudentSubscription row for every existing Student that
 * has a plan but no subscription row yet, using their current dateOfJoin + plan.subscriptionDays
 * as cycleStart/cycleEnd (the same formula the old computed-on-the-fly expiry used), so no
 * existing student loses their history when this feature ships.
 *
 * Guarded by app.migration.backfill-student-subscriptions (default true, runs once — it's a
 * no-op for any student that already has a subscription row, so it's safe to leave enabled).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StudentSubscriptionBackfillRunner implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final StudentSubscriptionRepository subscriptionRepository;

    @Value("${app.migration.backfill-student-subscriptions:true}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) return;

        List<Student> students = studentRepository.findAll();
        int created = 0;
        LocalDate today = LocalDate.now();

        for (Student student : students) {
            if (student.getPlan() == null) continue;
            if (subscriptionRepository.existsByStudentId(student.getId())) continue;

            Plan plan = student.getPlan();
            LocalDate start = student.getDateOfJoin() != null ? student.getDateOfJoin() : today;
            int durationDays = plan.getSubscriptionDays() != null && plan.getSubscriptionDays() > 0
                    ? plan.getSubscriptionDays()
                    : (plan.getDuration() != null && plan.getDuration() > 0 ? plan.getDuration().intValue() : 30);
            LocalDate cycleEnd = start.plusDays(durationDays);

            StudentSubscriptionStatus status = cycleEnd.isBefore(today)
                    ? StudentSubscriptionStatus.EXPIRED
                    : StudentSubscriptionStatus.ACTIVE;

            StudentSubscription sub = StudentSubscription.builder()
                    .student(student)
                    .plan(plan)
                    .library(student.getLibrary())
                    .cycleStart(start)
                    .cycleEnd(cycleEnd)
                    .planDurationDays(durationDays)
                    .planPrice(plan.getPrice())
                    .payable(plan.getPrice())
                    .paid(0.0)
                    .balance(plan.getPrice())
                    .carryForwardCredit(0.0)
                    .status(status)
                    .changeType(StudentSubscriptionChangeType.NEW)
                    .previousSubscriptionId(null)
                    .createdAt(LocalDateTime.now())
                    .createdBy("system-backfill")
                    .build();
            subscriptionRepository.save(sub);
            created++;
        }

        if (created > 0) {
            log.info("StudentSubscription backfill: created {} subscription row(s) for existing students", created);
        }
    }
}
