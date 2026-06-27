package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Enum.FeeStatus;
import com.learningJWT.LearningTemplate.Model.Fee;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeAuditLogDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeReceiptDTO;
import com.learningJWT.LearningTemplate.Repository.FeeRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.FeeReceiptService;
import com.learningJWT.LearningTemplate.Services.Impl.FeeAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/libraryadmin/reports")
@RequiredArgsConstructor
public class FeeReportController {

    private final FeeRepository feeRepository;
    private final UserRepository userRepository;
    private final FeeReceiptService feeReceiptService;
    private final FeeAuditLogService feeAuditLogService;

    private User getLoggedInAdmin() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
        }
        throw new Exception("No authenticated user");
    }

    /** Monthly collection report — all months with expected/collected/pending */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/monthly-collection")
    public ResponseEntity<?> monthlyCollection() {
        try {
            User admin = getLoggedInAdmin();
            Long libraryId = admin.getLibrary().getId();
            List<Fee> fees = feeRepository.findByLibraryId(libraryId);

            // Group by monthId
            Map<Integer, List<Fee>> byMonth = fees.stream()
                    .collect(Collectors.groupingBy(Fee::getMonthId));

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<Integer, List<Fee>> entry : byMonth.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
                int monthId = entry.getKey();
                List<Fee> mFees = entry.getValue();
                double expected = mFees.stream().mapToDouble(f -> f.getPayable() + f.getLateFee()).sum();
                double collected = mFees.stream().mapToDouble(Fee::getReceive).sum();
                double pending = mFees.stream().mapToDouble(f -> Math.max(0, f.getBalance())).sum();
                long paidCount = mFees.stream().filter(f -> f.getFeeStatus() == FeeStatus.PAID).count();
                long partialCount = mFees.stream().filter(f -> f.getFeeStatus() == FeeStatus.PARTIAL).count();
                long unpaidCount = mFees.stream().filter(f -> f.getFeeStatus() == FeeStatus.UNPAID).count();

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("monthId", monthId);
                row.put("expectedCollection", expected);
                row.put("collectedAmount", collected);
                row.put("pendingAmount", pending);
                row.put("collectionPct", expected > 0 ? Math.round((collected / expected) * 100) : 0);
                row.put("paidCount", paidCount);
                row.put("partialCount", partialCount);
                row.put("unpaidCount", unpaidCount);
                row.put("totalStudents", mFees.size());
                result.add(row);
            }

            // Dashboard summary cards
            double totalExpected = fees.stream().mapToDouble(f -> f.getPayable() + f.getLateFee()).sum();
            double totalCollected = fees.stream().mapToDouble(Fee::getReceive).sum();
            double totalPending = fees.stream().mapToDouble(f -> Math.max(0, f.getBalance())).sum();

            // Today's collection: fees paid today
            LocalDate today = LocalDate.now();
            double todayCollection = fees.stream()
                    .filter(f -> today.equals(f.getPaymentDate()))
                    .mapToDouble(Fee::getReceive).sum();

            long overdueStudents = fees.stream()
                    .filter(f -> f.getFeeStatus() != FeeStatus.PAID && f.getDueDate() != null && f.getDueDate().isBefore(today))
                    .map(f -> f.getStudent().getId()).distinct().count();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("months", result);
            response.put("totalExpected", totalExpected);
            response.put("totalCollected", totalCollected);
            response.put("totalPending", totalPending);
            response.put("todayCollection", todayCollection);
            response.put("overdueStudents", overdueStudents);
            response.put("activeStudents", fees.stream().map(f -> f.getStudent().getId()).distinct().count());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Student fee report with filters */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/student-fee-report")
    public ResponseEntity<?> studentFeeReport(
            @RequestParam(required = false) Integer monthId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String seatNumber) {
        try {
            User admin = getLoggedInAdmin();
            Long libraryId = admin.getLibrary().getId();
            List<Fee> fees = feeRepository.findByLibraryId(libraryId);

            // Apply filters
            if (monthId != null) fees = fees.stream().filter(f -> f.getMonthId() == monthId).collect(Collectors.toList());
            if (status != null && !status.isBlank()) {
                try {
                    FeeStatus fs = FeeStatus.valueOf(status.toUpperCase());
                    fees = fees.stream().filter(f -> f.getFeeStatus() == fs).collect(Collectors.toList());
                } catch (Exception ignored) {}
            }
            if (search != null && !search.isBlank()) {
                String q = search.toLowerCase();
                fees = fees.stream().filter(f -> {
                    Student s = f.getStudent();
                    if (s == null) return false;
                    return (s.getFullName() != null && s.getFullName().toLowerCase().contains(q))
                            || (s.getPhone() != null && s.getPhone().contains(q))
                            || (s.getAdmissionNumber() != null && s.getAdmissionNumber().toLowerCase().contains(q));
                }).collect(Collectors.toList());
            }
            if (seatNumber != null && !seatNumber.isBlank()) {
                fees = fees.stream().filter(f -> {
                    Student s = f.getStudent();
                    return s != null && s.getSeat() != null &&
                            s.getSeat().getSeatName() != null &&
                            s.getSeat().getSeatName().equalsIgnoreCase(seatNumber);
                }).collect(Collectors.toList());
            }

            List<Map<String, Object>> rows = fees.stream().map(f -> {
                Student s = f.getStudent();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("feeId", f.getFeeId());
                row.put("studentId", s != null ? s.getId() : null);
                row.put("studentName", s != null ? s.getFullName() : null);
                row.put("admissionNumber", s != null ? s.getAdmissionNumber() : null);
                row.put("phone", s != null ? s.getPhone() : null);
                row.put("seatNumber", s != null && s.getSeat() != null ? s.getSeat().getSeatName() : null);
                row.put("plan", s != null && s.getPlan() != null ? s.getPlan().getName() : null);
                row.put("monthId", f.getMonthId());
                row.put("monthlyFee", f.getPayable());
                row.put("lateFee", f.getLateFee());
                row.put("concession", f.getConcession());
                row.put("paidAmount", f.getReceive());
                row.put("remainingAmount", f.getBalance());
                row.put("status", f.getFeeStatus());
                row.put("dueDate", f.getDueDate());
                row.put("paymentDate", f.getPaymentDate());
                return row;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Pending dues report — sorted by highest pending */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/pending-dues")
    public ResponseEntity<?> pendingDues() {
        try {
            User admin = getLoggedInAdmin();
            Long libraryId = admin.getLibrary().getId();
            List<Fee> fees = feeRepository.findByLibraryId(libraryId);
            LocalDate today = LocalDate.now();

            List<Map<String, Object>> rows = fees.stream()
                    .filter(f -> f.getFeeStatus() != FeeStatus.PAID && f.getBalance() > 0)
                    .sorted((a, b) -> Double.compare(b.getBalance(), a.getBalance()))
                    .map(f -> {
                        Student s = f.getStudent();
                        long daysOverdue = f.getDueDate() != null ? java.time.temporal.ChronoUnit.DAYS.between(f.getDueDate(), today) : 0;
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("studentId", s != null ? s.getId() : null);
                        row.put("studentName", s != null ? s.getFullName() : null);
                        row.put("phone", s != null ? s.getPhone() : null);
                        row.put("seatNumber", s != null && s.getSeat() != null ? s.getSeat().getSeatName() : null);
                        row.put("monthId", f.getMonthId());
                        row.put("pendingAmount", f.getBalance());
                        row.put("paidAmount", f.getReceive());
                        row.put("payable", f.getPayable());
                        row.put("lastPaymentDate", f.getPaymentDate());
                        row.put("dueDate", f.getDueDate());
                        row.put("daysOverdue", Math.max(0, daysOverdue));
                        row.put("status", f.getFeeStatus());
                        return row;
                    }).collect(Collectors.toList());

            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Defaulter report — 30/60/90 day buckets */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/defaulters")
    public ResponseEntity<?> defaulters() {
        try {
            User admin = getLoggedInAdmin();
            Long libraryId = admin.getLibrary().getId();
            List<Fee> fees = feeRepository.findByLibraryId(libraryId);
            LocalDate today = LocalDate.now();

            List<Map<String, Object>> d30 = new ArrayList<>();
            List<Map<String, Object>> d60 = new ArrayList<>();
            List<Map<String, Object>> d90 = new ArrayList<>();

            for (Fee f : fees) {
                if (f.getFeeStatus() == FeeStatus.PAID) continue;
                if (f.getDueDate() == null) continue;
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(f.getDueDate(), today);
                if (daysOverdue <= 0) continue;

                Student s = f.getStudent();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("studentId", s != null ? s.getId() : null);
                row.put("studentName", s != null ? s.getFullName() : null);
                row.put("phone", s != null ? s.getPhone() : null);
                row.put("seatNumber", s != null && s.getSeat() != null ? s.getSeat().getSeatName() : null);
                row.put("pendingAmount", f.getBalance());
                row.put("daysOverdue", daysOverdue);
                row.put("dueDate", f.getDueDate());
                row.put("status", f.getFeeStatus());

                if (daysOverdue >= 90) d90.add(row);
                else if (daysOverdue >= 60) d60.add(row);
                else d30.add(row);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("days30", d30);
            result.put("days60", d60);
            result.put("days90", d90);
            result.put("totalDefaulters", d30.size() + d60.size() + d90.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Receipts — admin view */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/receipts")
    public ResponseEntity<?> getReceipts() {
        try {
            return ResponseEntity.ok(feeReceiptService.getLibraryReceipts());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/receipts/student/{studentId}")
    public ResponseEntity<?> getStudentReceipts(@PathVariable Long studentId) {
        try {
            return ResponseEntity.ok(feeReceiptService.getStudentReceipts(studentId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/receipts/{id}")
    public ResponseEntity<?> getReceiptById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(feeReceiptService.getReceiptById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Audit log */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/audit-log")
    public ResponseEntity<?> getAuditLog() {
        try {
            User admin = getLoggedInAdmin();
            return ResponseEntity.ok(feeAuditLogService.getLibraryAuditLogs(admin.getLibrary().getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Student fee ledger */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/ledger/{studentId}")
    public ResponseEntity<?> getStudentLedger(@PathVariable Long studentId) {
        try {
            User admin = getLoggedInAdmin();
            List<Fee> fees = feeRepository.findByStudentIdAndLibraryId(studentId, admin.getLibrary().getId());
            fees.sort(Comparator.comparingInt(Fee::getMonthId));

            double runningBalance = 0;
            List<Map<String, Object>> entries = new ArrayList<>();

            for (Fee f : fees) {
                // Debit: monthly fee added
                Map<String, Object> debit = new LinkedHashMap<>();
                debit.put("date", f.getDueDate());
                debit.put("description", "Monthly Fee - " + monthLabel(f.getMonthId()));
                debit.put("debit", f.getPayable());
                debit.put("credit", 0);
                runningBalance += f.getPayable();
                debit.put("balance", runningBalance);
                debit.put("type", "DEBIT");
                entries.add(debit);

                if (f.getLateFee() > 0) {
                    Map<String, Object> late = new LinkedHashMap<>();
                    late.put("date", f.getDueDate());
                    late.put("description", "Late Fee - " + monthLabel(f.getMonthId()));
                    late.put("debit", f.getLateFee());
                    late.put("credit", 0);
                    runningBalance += f.getLateFee();
                    late.put("balance", runningBalance);
                    late.put("type", "DEBIT");
                    entries.add(late);
                }

                if (f.getConcession() > 0) {
                    Map<String, Object> con = new LinkedHashMap<>();
                    con.put("date", f.getPaymentDate() != null ? f.getPaymentDate() : f.getDueDate());
                    con.put("description", "Concession - " + monthLabel(f.getMonthId()));
                    con.put("debit", 0);
                    con.put("credit", f.getConcession());
                    runningBalance -= f.getConcession();
                    con.put("balance", runningBalance);
                    con.put("type", "CREDIT");
                    entries.add(con);
                }

                if (f.getReceive() > 0) {
                    Map<String, Object> credit = new LinkedHashMap<>();
                    credit.put("date", f.getPaymentDate() != null ? f.getPaymentDate() : f.getDueDate());
                    credit.put("description", "Fee Paid - " + monthLabel(f.getMonthId()));
                    credit.put("debit", 0);
                    credit.put("credit", f.getReceive());
                    runningBalance -= f.getReceive();
                    credit.put("balance", Math.max(0, runningBalance));
                    credit.put("type", "CREDIT");
                    entries.add(credit);
                    runningBalance = Math.max(0, runningBalance);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("entries", entries);
            result.put("currentBalance", runningBalance);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String monthLabel(int monthId) {
        String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        if (monthId >= 1 && monthId <= 12) return months[monthId];
        return String.valueOf(monthId);
    }
}
