package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Enum.FeeStatus;
import com.learningJWT.LearningTemplate.Enum.PlanDurationType;
import com.learningJWT.LearningTemplate.Enum.StudentSubscriptionChangeType;
import com.learningJWT.LearningTemplate.Enum.StudentSubscriptionStatus;
import com.learningJWT.LearningTemplate.Enum.SubscriptionChangeMode;
import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.StudentSubscriptionDTO;
import com.learningJWT.LearningTemplate.Repository.FeeRepository;
import com.learningJWT.LearningTemplate.Repository.PlanRepository;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Repository.StudentSubscriptionRepository;
import com.learningJWT.LearningTemplate.Services.StudentSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentSubscriptionServiceImpl implements StudentSubscriptionService {

    private final StudentSubscriptionRepository subscriptionRepository;
    private final StudentRepository studentRepository;
    private final PlanRepository planRepository;
    private final FeeRepository feeRepository;

    private static final int EXPIRING_SOON_WINDOW_DAYS = 3;

    private String currentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return "system";
    }

    private int resolveDurationDays(Plan plan) {
        if (plan.getSubscriptionDays() != null && plan.getSubscriptionDays() > 0) {
            return plan.getSubscriptionDays();
        }
        if (plan.getDuration() != null && plan.getDuration() > 0) {
            return plan.getDuration().intValue();
        }
        return 30; // sane fallback so a mis-configured plan never produces a zero/negative cycle
    }

    /**
     * Month-based plans (dropdown: 1/2/3/4/6/12 months) always expire on the same date
     * N month(s) later — e.g. start 5 Jan + 1 month = 5 Feb, start 31 Jan + 1 month = 28/29 Feb
     * (LocalDate#plusMonths clamps to the last valid day of the target month automatically).
     * Custom/day-count plans keep the old start.plusDays(durationDays) behaviour.
     */
    private LocalDate computeCycleEnd(LocalDate start, Plan plan) {
        if (plan.getDurationType() == PlanDurationType.MONTHS
                && plan.getDurationMonths() != null && plan.getDurationMonths() > 0) {
            return start.plusMonths(plan.getDurationMonths());
        }
        return start.plusDays(resolveDurationDays(plan));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentSubscription createInitialSubscription(Student student, Plan plan, LocalDate startDate) throws Exception {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate cycleEnd = computeCycleEnd(start, plan);
        int durationDays = (int) ChronoUnit.DAYS.between(start, cycleEnd);

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
                .status(StudentSubscriptionStatus.ACTIVE)
                .changeType(StudentSubscriptionChangeType.NEW)
                .previousSubscriptionId(null)
                .createdAt(LocalDateTime.now())
                .createdBy(currentUsername())
                .build();
        sub = subscriptionRepository.save(sub);

        createFeeForCycle(student, sub, start);

        student.setPlan(plan);
        studentRepository.save(student);

        return sub;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentSubscription changePlan(Long studentId, Long newPlanId, SubscriptionChangeMode modeIn) throws Exception {
        SubscriptionChangeMode mode = modeIn != null ? modeIn : SubscriptionChangeMode.EXTEND;

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new Exception("Student not found"));
        Plan newPlan = planRepository.findById(newPlanId)
                .orElseThrow(() -> new Exception("Plan not found"));

        Optional<StudentSubscription> activeOpt =
                subscriptionRepository.findFirstByStudentIdAndStatusOrderByCycleStartDesc(
                        studentId, StudentSubscriptionStatus.ACTIVE);

        LocalDate today = LocalDate.now();
        LocalDate newStart;
        double carryForward = 0.0;
        Long previousId = null;

        if (activeOpt.isPresent()) {
            StudentSubscription current = activeOpt.get();
            previousId = current.getId();

            if (mode == SubscriptionChangeMode.EXTEND) {
                newStart = current.getCycleEnd().isAfter(today) ? current.getCycleEnd().plusDays(1) : today;
                current.setStatus(StudentSubscriptionStatus.RENEWED);
            } else { // REPLACE
                newStart = today;
                if (current.getCycleEnd().isAfter(today) && current.getPlanDurationDays() != null
                        && current.getPlanDurationDays() > 0 && current.getPlanPrice() != null) {
                    long remainingDays = ChronoUnit.DAYS.between(today, current.getCycleEnd());
                    carryForward = (remainingDays / (double) current.getPlanDurationDays()) * current.getPlanPrice();
                    carryForward = Math.round(carryForward * 100.0) / 100.0;
                }
                current.setStatus(StudentSubscriptionStatus.UPGRADED);
            }
            subscriptionRepository.save(current);
        } else {
            // No active cycle (either a brand new student, or their last cycle already expired) —
            // still route through here so history/changeType stays consistent.
            newStart = today;
        }

        LocalDate newCycleEnd = computeCycleEnd(newStart, newPlan);
        int durationDays = (int) ChronoUnit.DAYS.between(newStart, newCycleEnd);
        double payable = Math.max(0.0, newPlan.getPrice() - carryForward);

        StudentSubscription newSub = StudentSubscription.builder()
                .student(student)
                .plan(newPlan)
                .library(student.getLibrary())
                .cycleStart(newStart)
                .cycleEnd(newCycleEnd)
                .planDurationDays(durationDays)
                .planPrice(newPlan.getPrice())
                .payable(payable)
                .paid(0.0)
                .balance(payable)
                .carryForwardCredit(carryForward)
                .status(StudentSubscriptionStatus.ACTIVE)
                .changeType(mode == SubscriptionChangeMode.EXTEND
                        ? StudentSubscriptionChangeType.RENEWAL
                        : StudentSubscriptionChangeType.UPGRADE)
                .previousSubscriptionId(previousId)
                .createdAt(LocalDateTime.now())
                .createdBy(currentUsername())
                .build();
        newSub = subscriptionRepository.save(newSub);

        createFeeForCycle(student, newSub, newStart);

        student.setPlan(newPlan);
        studentRepository.save(student);

        return newSub;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentSubscription ensureSubscriptionForAllocation(Student student, Plan plan) throws Exception {
        Optional<StudentSubscription> activeOpt =
                subscriptionRepository.findFirstByStudentIdAndStatusOrderByCycleStartDesc(
                        student.getId(), StudentSubscriptionStatus.ACTIVE);

        if (activeOpt.isEmpty()) {
            return createInitialSubscription(student, plan, LocalDate.now());
        }
        StudentSubscription active = activeOpt.get();
        if (active.getPlan() != null && active.getPlan().getId().equals(plan.getId())) {
            // Same plan already active — nothing to do, avoid creating a duplicate cycle/invoice.
            student.setPlan(plan);
            studentRepository.save(student);
            return active;
        }
        return changePlan(student.getId(), plan.getId(), SubscriptionChangeMode.EXTEND);
    }

    private void createFeeForCycle(Student student, StudentSubscription sub, LocalDate cycleStart) {
        Fee fee = new Fee();
        fee.setStudent(student);
        fee.setLibrary(student.getLibrary());
        fee.setSubscription(sub);
        fee.setFeeStatus(FeeStatus.UNPAID);
        fee.setDueDate(cycleStart);
        fee.setMonthId(cycleStart.getMonthValue());
        fee.setPayable(sub.getPayable() != null ? sub.getPayable() : 0.0);
        fee.setReceive(0);
        fee.setConcession(0);
        fee.setBalance(sub.getPayable() != null ? sub.getPayable() : 0.0);
        fee.setLateFee(0);
        Fee saved = feeRepository.save(fee);
        // keep subscription's monetary snapshot in sync with the invoice id used to create it
        sub.setPayable(saved.getPayable());
        sub.setBalance(saved.getBalance());
    }

    @Override
    public Optional<StudentSubscription> getActiveSubscription(Long studentId) {
        return subscriptionRepository.findFirstByStudentIdAndStatusOrderByCycleStartDesc(
                studentId, StudentSubscriptionStatus.ACTIVE);
    }

    @Override
    public List<StudentSubscription> getSubscriptionHistory(Long studentId) {
        return subscriptionRepository.findByStudentIdOrderByCycleStartDesc(studentId);
    }

    @Override
    public StudentSubscriptionStatus computeDisplayStatus(StudentSubscription sub) {
        if (sub.getStatus() != StudentSubscriptionStatus.ACTIVE) {
            return sub.getStatus();
        }
        LocalDate today = LocalDate.now();
        if (sub.getCycleEnd().isBefore(today)) {
            return StudentSubscriptionStatus.EXPIRED;
        }
        long daysLeft = ChronoUnit.DAYS.between(today, sub.getCycleEnd());
        if (daysLeft <= EXPIRING_SOON_WINDOW_DAYS) {
            return StudentSubscriptionStatus.EXPIRING_SOON;
        }
        return StudentSubscriptionStatus.ACTIVE;
    }

    @Override
    public StudentSubscriptionDTO toDTO(StudentSubscription sub) {
        LocalDate today = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(today, sub.getCycleEnd());
        Long feeId = null;
        List<Fee> fees = feeRepository.findByStudentIdAndLibraryId(
                sub.getStudent().getId(), sub.getLibrary() != null ? sub.getLibrary().getId() : null);
        for (Fee f : fees) {
            if (f.getSubscription() != null && f.getSubscription().getId().equals(sub.getId())) {
                feeId = f.getFeeId();
                break;
            }
        }
        return StudentSubscriptionDTO.builder()
                .id(sub.getId())
                .studentId(sub.getStudent() != null ? sub.getStudent().getId() : null)
                .studentName(sub.getStudent() != null ? sub.getStudent().getFullName() : null)
                .planId(sub.getPlan() != null ? sub.getPlan().getId() : null)
                .planName(sub.getPlan() != null ? sub.getPlan().getName() : null)
                .cycleStart(sub.getCycleStart())
                .cycleEnd(sub.getCycleEnd())
                .planDurationDays(sub.getPlanDurationDays())
                .planPrice(sub.getPlanPrice())
                .payable(sub.getPayable())
                .paid(sub.getPaid())
                .balance(sub.getBalance())
                .carryForwardCredit(sub.getCarryForwardCredit())
                .status(sub.getStatus())
                .displayStatus(computeDisplayStatus(sub))
                .changeType(sub.getChangeType())
                .previousSubscriptionId(sub.getPreviousSubscriptionId())
                .daysRemaining(daysRemaining)
                .createdAt(sub.getCreatedAt())
                .createdBy(sub.getCreatedBy())
                .feeId(feeId)
                .build();
    }

    @Override
    public void enrichStudentDTO(StudentDTO dto, Long studentId) {
        Optional<StudentSubscription> activeOpt = getActiveSubscription(studentId);
        if (activeOpt.isEmpty()) return;
        StudentSubscription sub = activeOpt.get();
        dto.setSubscriptionExpiryDate(sub.getCycleEnd());
        dto.setSubscriptionCycleStart(sub.getCycleStart());
        dto.setSubscriptionCycleEnd(sub.getCycleEnd());
        dto.setSubscriptionDaysRemaining(ChronoUnit.DAYS.between(LocalDate.now(), sub.getCycleEnd()));
        dto.setSubscriptionStatus(computeDisplayStatus(sub).name());
        dto.setSubscriptionPayable(sub.getPayable());
        dto.setSubscriptionPaid(sub.getPaid());
        dto.setSubscriptionBalance(sub.getBalance());
    }
}
