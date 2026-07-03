package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Mapper.FeeMapper;
import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeReceiptDTO;
import com.learningJWT.LearningTemplate.Repository.*;
import com.learningJWT.LearningTemplate.Services.FeeReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeReceiptServiceImpl implements FeeReceiptService {

    private final FeeReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    private User getLoggedInUser() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
        }
        throw new Exception("No authenticated user");
    }

    private Student getLoggedInStudent() throws Exception {
        User user = getLoggedInUser();
        Student student = studentRepository.findByUserId(user.getId());
        if (student == null) throw new Exception("Student not found");
        return student;
    }

    /**
     * Builds a receipt number that is unique *within this library* (format:
     * RCPT-{libraryCode}-{YYYY}-{NNNNN}). Library code is embedded so numbers can never
     * collide across libraries even though the DB constraint is (library_id, receipt_number).
     * Retries a few times on a rare concurrent-double-submit race (two requests computing
     * the same next sequence at once) before giving up.
     */
    private String generateUniqueReceiptNumber(Library library) {
        String libTag = (library.getLibraryCode() != null && !library.getLibraryCode().isBlank())
                ? library.getLibraryCode()
                : "L" + library.getId();
        int year = LocalDate.now().getYear();
        String yearTag = "-" + year + "-";

        int maxAttempts = 5;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            long seq = receiptRepository.countByLibraryIdAndYearTag(library.getId(), yearTag) + 1 + attempt;
            String candidate = String.format("RCPT-%s-%d-%05d", libTag, year, seq);
            if (!receiptRepository.existsByLibraryIdAndReceiptNumber(library.getId(), candidate)) {
                return candidate;
            }
        }
        // Extremely unlikely fallback: timestamp suffix guarantees uniqueness.
        return String.format("RCPT-%s-%d-%d", libTag, year, System.currentTimeMillis());
    }

    @Override
    public FeeReceiptDTO generateReceipt(Fee fee, PaymentProof proof, double amountPaid,
                                          double concession, double lateFee,
                                          String paymentMode, String transactionRef) throws Exception {
        Library library = fee.getLibrary();
        Student student = fee.getStudent();

        FeeReceipt saved = null;
        Exception lastError = null;

        // Retry loop: guards against the rare race where two concurrent requests for the
        // same library compute the same "next" receipt number at the same instant.
        for (int attempt = 0; attempt < 3 && saved == null; attempt++) {
            String receiptNo = generateUniqueReceiptNumber(library);
            FeeReceipt receipt = FeeReceipt.builder()
                    .receiptNumber(receiptNo)
                    .fee(fee)
                    .student(student)
                    .library(library)
                    .amountPaid(amountPaid)
                    .concession(concession)
                    .lateFee(lateFee)
                    .balanceAfter(fee.getBalance())
                    .paymentMode(paymentMode != null ? paymentMode : "UPI/Online")
                    .transactionRef(transactionRef)
                    .paymentDate(LocalDate.now())
                    .generatedAt(LocalDateTime.now())
                    .proof(proof)
                    .build();
            try {
                saved = receiptRepository.saveAndFlush(receipt);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                lastError = e; // duplicate on this attempt — loop and regenerate
            }
        }

        if (saved == null) {
            throw new Exception("Could not generate a unique receipt number, please retry", lastError);
        }
        return toDTO(saved);
    }

    @Override
    public List<FeeReceiptDTO> getMyReceipts() throws Exception {
        Student student = getLoggedInStudent();
        return receiptRepository.findByStudentIdOrderByGeneratedAtDesc(student.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public FeeReceiptDTO getReceiptById(Long id) throws Exception {
        FeeReceipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new Exception("Receipt not found"));
        return toDTO(receipt);
    }

    @Override
    public List<FeeReceiptDTO> getLibraryReceipts() throws Exception {
        User admin = getLoggedInUser();
        return receiptRepository.findByLibraryIdOrderByGeneratedAtDesc(admin.getLibrary().getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<FeeReceiptDTO> getStudentReceipts(Long studentId) throws Exception {
        return receiptRepository.findByStudentIdOrderByGeneratedAtDesc(studentId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private FeeReceiptDTO toDTO(FeeReceipt r) {
        Library lib = r.getLibrary();
        Student st = r.getStudent();
        Fee fee = r.getFee();

        return FeeReceiptDTO.builder()
                .id(r.getId())
                .receiptNumber(r.getReceiptNumber())
                .libraryName(lib != null ? lib.getName() : null)
                .libraryAddress(lib != null ? lib.getAddress() : null)
                .libraryPhone(lib != null ? lib.getPhone() : null)
                .libraryGst(lib != null ? lib.getGstNumber() : null)
                .studentName(st != null ? st.getFullName() : null)
                .admissionNumber(st != null ? st.getAdmissionNumber() : null)
                .studentPhone(st != null ? st.getPhone() : null)
                .seatNumber(st != null && st.getSeat() != null ? st.getSeat().getSeatName() : null)
                .membershipPlan(st != null && st.getPlan() != null ? st.getPlan().getName() : null)
                .studentId(st != null ? st.getId() : null)
                .paymentDate(r.getPaymentDate())
                .paymentMode(r.getPaymentMode())
                .transactionRef(r.getTransactionRef())
                .monthlyFee(fee != null ? fee.getPayable() : 0)
                .lateFee(r.getLateFee())
                .concession(r.getConcession())
                .amountPaid(r.getAmountPaid())
                .balanceAfter(r.getBalanceAfter())
                .monthId(fee != null ? fee.getMonthId() : 0)
                .fee(fee != null ? FeeMapper.toDTO(fee) : null)
                .generatedAt(r.getGeneratedAt())
                .proofId(r.getProof() != null ? r.getProof().getId() : null)
                .build();
    }
}
