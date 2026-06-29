package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Enum.FeeStatus;
import com.learningJWT.LearningTemplate.Model.Fee;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeReceiptDTO;
import com.learningJWT.LearningTemplate.Repository.FeeRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.FeeReceiptService;
import com.learningJWT.LearningTemplate.Services.Impl.FeeAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof UserDetails ud)
            return userRepository.findByUsername(ud.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
        throw new Exception("No authenticated user");
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/monthly-collection")
    public ResponseEntity<?> monthlyCollection() {
        try {
            User admin = getLoggedInAdmin();
            Long libraryId = admin.getLibrary().getId();
            // Scoped to this library only — no findAll()
            List<Fee> fees = feeRepository.findByLibraryId(libraryId);

            Map<Integer, List<Fee>> byMonth = fees.stream()
                    .collect(Collectors.groupingBy(Fee::getMonthId));

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<Integer, List<Fee>> entry : byMonth.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).toList()) {
                int monthId = entry.getKey();
                List<Fee> mFees = entry.getValue();
                double expected = mFees.stream().mapToDouble(f -> f.getPayable() + f.getLateFee()).sum();
                double collected = mFees.stream().mapToDouble(Fee::getReceive).sum();
                double pending = mFees.stream().mapToDouble(f -> Math.max(0, f.getBalance())).sum();

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("monthId",           monthId);
                row.put("expectedCollection", expected);
                row.put("collectedAmount",    collected);
                row.put("pendingAmount",      pending);
                row.put("collectionPct",      expected > 0 ? Math.round((collected / expected) * 100) : 0);
                row.put("paidCount",    mFees.stream().filter(f -> f.getFeeStatus() == FeeStatus.PAID).count());
                row.put("partialCount", mFees.stream().filter(f -> f.getFeeStatus() == FeeStatus.PARTIAL).count());
                row.put("unpaidCount",  mFees.stream().filter(f -> f.getFeeStatus() == FeeStatus.UNPAID).count());
                row.put("totalStudents", mFees.size());
                result.add(row);
            }

            LocalDate today = LocalDate.now();
            double todayCollection = fees.stream()
                    .filter(f -> today.equals(f.getPaymentDate()))
                    .mapToDouble(Fee::getReceive).sum();

            long overdueStudents = fees.stream()
                    .filter(f -> f.getFeeStatus() != FeeStatus.PAID
                            && f.getDueDate() != null && f.getDueDate().isBefore(today))
                    .map(f -> f.getStudent().getId()).distinct().count();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("months",           result);
            response.put("totalExpected",     fees.stream().mapToDouble(f -> f.getPayable() + f.getLateFee()).sum());
            response.put("totalCollected",    fees.stream().mapToDouble(Fee::getReceive).sum());
            response.put("totalPending",      fees.stream().mapToDouble(f -> Math.max(0, f.getBalance())).sum());
            response.put("todayCollection",   todayCollection);
            response.put("overdueStudents",   overdueStudents);
            response.put("activeStudents",    fees.stream().map(f -> f.getStudent().getId()).distinct().count());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/student-fee-report")
    public ResponseEntity<?> studentFeeReport(
            @RequestParam(required = false) Integer monthId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            User admin = getLoggedInAdmin();
            Long libraryId = admin.getLibrary().getId();

            List<Fee> fees;
            if (search != null && !search.isBlank()) {
                Pageable pageable = PageRequest.of(page, size);
                fees = feeRepository.searchByLibrary(libraryId, search, pageable).getContent();
            } else {
                fees = feeRepository.findByLibraryId(libraryId);
            }

            // In-memory filters for small datasets
            if (monthId != null) fees = fees.stream().filter(f -> f.getMonthId() == monthId).toList();
            if (status != null && !status.isBlank()) {
                try {
                    FeeStatus fs = FeeStatus.valueOf(status.toUpperCase());
                    fees = fees.stream().filter(f -> f.getFeeStatus() == fs).toList();
                } catch (Exception ignored) {}
            }

            List<Map<String, Object>> rows = fees.stream().map(f -> {
                Student s = f.getStudent();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("feeId",           f.getFeeId());
                row.put("studentId",        s != null ? s.getId() : null);
                row.put("studentName",      s != null ? s.getFullName() : null);
                row.put("admissionNumber",  s != null ? s.getAdmissionNumber() : null);
                row.put("phone",            s != null ? s.getPhone() : null);
                row.put("seatNumber",       s != null && s.getSeat() != null ? s.getSeat().getSeatName() : null);
                row.put("plan",             s != null && s.getPlan() != null ? s.getPlan().getName() : null);
                row.put("monthId",          f.getMonthId());
                row.put("monthlyFee",       f.getPayable());
                row.put("lateFee",          f.getLateFee());
                row.put("concession",       f.getConcession());
                row.put("paidAmount",       f.getReceive());
                row.put("remainingAmount",  f.getBalance());
                row.put("status",           f.getFeeStatus());
                row.put("dueDate",          f.getDueDate());
                row.put("paymentDate",      f.getPaymentDate());
                return row;
            }).toList();

            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/pending-dues")
    public ResponseEntity<?> pendingDues() {
        try {
            User admin = getLoggedInAdmin();
            Long libraryId = admin.getLibrary().getId();
            // Use optimized query
            List<Fee> fees = feeRepository.findPendingFeesByLibrary(libraryId);
            LocalDate today = LocalDate.now();

            List<Map<String, Object>> rows = fees.stream().map(f -> {
                Student s = f.getStudent();
                long daysOverdue = f.getDueDate() != null
                        ? java.time.temporal.ChronoUnit.DAYS.between(f.getDueDate(), today) : 0;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("studentId",    s != null ? s.getId() : null);
                row.put("studentName",  s != null ? s.getFullName() : null);
                row.put("phone",        s != null ? s.getPhone() : null);
                row.put("seatNumber",   s != null && s.getSeat() != null ? s.getSeat().getSeatName() : null);
                row.put("monthId",      f.getMonthId());
                row.put("pendingAmount", f.getBalance());
                row.put("paidAmount",   f.getReceive());
                row.put("payable",      f.getPayable());
                row.put("lastPaymentDate", f.getPaymentDate());
                row.put("dueDate",      f.getDueDate());
                row.put("daysOverdue",  Math.max(0, daysOverdue));
                row.put("status",       f.getFeeStatus());
                return row;
            }).toList();

            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/defaulters")
    public ResponseEntity<?> defaulters() {
        try {
            User admin = getLoggedInAdmin();
            Long libraryId = admin.getLibrary().getId();
            List<Fee> fees = feeRepository.findPendingFeesByLibrary(libraryId);
            LocalDate today = LocalDate.now();

            List<Map<String, Object>> d30 = new ArrayList<>(), d60 = new ArrayList<>(), d90 = new ArrayList<>();
            for (Fee f : fees) {
                if (f.getDueDate() == null) continue;
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(f.getDueDate(), today);
                if (daysOverdue <= 0) continue;
                Student s = f.getStudent();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("studentId",    s != null ? s.getId() : null);
                row.put("studentName",  s != null ? s.getFullName() : null);
                row.put("phone",        s != null ? s.getPhone() : null);
                row.put("seatNumber",   s != null && s.getSeat() != null ? s.getSeat().getSeatName() : null);
                row.put("pendingAmount", f.getBalance());
                row.put("daysOverdue",  daysOverdue);
                row.put("dueDate",      f.getDueDate());
                row.put("status",       f.getFeeStatus());
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

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/ledger/{studentId}")
    public ResponseEntity<?> getStudentLedger(@PathVariable Long studentId) {
        try {
            User admin = getLoggedInAdmin();
            List<Fee> fees = feeRepository.findByStudentIdAndLibraryId(studentId, admin.getLibrary().getId());
            fees = fees.stream().sorted(Comparator.comparingInt(Fee::getMonthId)).toList();

            double runningBalance = 0;
            List<Map<String, Object>> entries = new ArrayList<>();
            String[] months = {"","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

            for (Fee f : fees) {
                String ml = (f.getMonthId() >= 1 && f.getMonthId() <= 12) ? months[f.getMonthId()] : String.valueOf(f.getMonthId());
                runningBalance += f.getPayable();
                entries.add(entry(f.getDueDate(), "Monthly Fee - " + ml, f.getPayable(), 0, runningBalance, "DEBIT"));
                if (f.getLateFee() > 0) {
                    runningBalance += f.getLateFee();
                    entries.add(entry(f.getDueDate(), "Late Fee - " + ml, f.getLateFee(), 0, runningBalance, "DEBIT"));
                }
                if (f.getConcession() > 0) {
                    runningBalance -= f.getConcession();
                    entries.add(entry(f.getPaymentDate() != null ? f.getPaymentDate() : f.getDueDate(), "Concession - " + ml, 0, f.getConcession(), runningBalance, "CREDIT"));
                }
                if (f.getReceive() > 0) {
                    runningBalance = Math.max(0, runningBalance - f.getReceive());
                    entries.add(entry(f.getPaymentDate() != null ? f.getPaymentDate() : f.getDueDate(), "Fee Paid - " + ml, 0, f.getReceive(), runningBalance, "CREDIT"));
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

    private Map<String, Object> entry(Object date, String desc, double debit, double credit, double balance, String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", date); m.put("description", desc);
        m.put("debit", debit); m.put("credit", credit);
        m.put("balance", balance); m.put("type", type);
        return m;
    }
}
