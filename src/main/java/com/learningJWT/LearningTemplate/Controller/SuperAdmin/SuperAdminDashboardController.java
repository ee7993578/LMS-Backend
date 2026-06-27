package com.learningJWT.LearningTemplate.Controller.SuperAdmin;

import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Enum.TicketStatus;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Repository.*;
import com.learningJWT.LearningTemplate.Services.SupportTicketService;
import com.learningJWT.LearningTemplate.Services.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/superadmin/dashboard")
@RequiredArgsConstructor
public class SuperAdminDashboardController {

    private final LibraryRepository libraryRepository;
    private final StudentRepository studentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SupportTicketService ticketService;

    @GetMapping
    public ResponseEntity<?> dashboard() {
        List<Library> libraries = libraryRepository.findAll();

        long total     = libraries.size();
        long active    = libraries.stream().filter(l -> l.getStatus() == Status.ACTIVE).count();
        long trial     = libraries.stream().filter(l -> l.getStatus() == Status.TRIAL).count();
        long suspended = libraries.stream().filter(l ->
                l.getStatus() == Status.SUSPENDED || l.getStatus() == Status.INACTIVE).count();
        long expired   = libraries.stream().filter(l -> l.getStatus() == Status.EXPIRED
                || l.getStatus() == Status.EXPIRED_READ_ONLY).count();

        // Revenue: sum across all subscriptions (not fee records, no cross-tenant leak)
        double totalRevenue = subscriptionRepository.findAll().stream()
                .filter(s -> s.getPricePerMonth() != null)
                .mapToDouble(s -> {
                    if (s.getBillingStart() == null || s.getBillingEnd() == null) return 0;
                    long months = java.time.temporal.ChronoUnit.MONTHS.between(
                            s.getBillingStart(), s.getBillingEnd());
                    return months * s.getPricePerMonth();
                }).sum();

        // This month revenue (libraries whose billing is active this month)
        int curMonth = LocalDate.now().getMonthValue();
        int curYear  = LocalDate.now().getYear();
        double monthRevenue = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == Status.ACTIVE && s.getPricePerMonth() != null)
                .mapToDouble(s -> s.getPricePerMonth()).sum();

        long totalStudents = studentRepository.count();

        // Monthly library growth (last 6 months)
        Map<String, Long> libraryGrowth = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = LocalDate.now().minusMonths(i);
            String label = month.getMonth().name().substring(0, 3) + " " + month.getYear();
            long count = libraries.stream().filter(l ->
                    l.getCreatedAt() != null &&
                    l.getCreatedAt().getMonthValue() == month.getMonthValue() &&
                    l.getCreatedAt().getYear() == month.getYear()).count();
            libraryGrowth.put(label, count);
        }

        // Plan distribution
        Map<String, Long> planDist = libraries.stream()
                .filter(l -> l.getLibraryPlan() != null)
                .collect(Collectors.groupingBy(l -> l.getLibraryPlan().getName(), Collectors.counting()));

        // Status distribution
        Map<String, Long> statusDist = libraries.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getStatus() != null ? l.getStatus().name() : "UNKNOWN",
                        Collectors.counting()));

        Map<String, Long> ticketStats = ticketService.getStats();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalLibraries", total);
        result.put("activeLibraries", active);
        result.put("trialLibraries", trial);
        result.put("suspendedLibraries", suspended);
        result.put("expiredLibraries", expired);
        result.put("totalRevenue", totalRevenue);
        result.put("monthlyRevenue", monthRevenue);
        result.put("totalStudents", totalStudents);
        result.put("libraryGrowth", libraryGrowth);
        result.put("planDistribution", planDist);
        result.put("statusDistribution", statusDist);
        result.put("ticketStats", ticketStats);
        result.put("openTickets", ticketStats.getOrDefault(TicketStatus.OPEN.name(), 0L));

        return ResponseEntity.ok(result);
    }
}
