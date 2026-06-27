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

    @Override
    public FeeReceiptDTO generateReceipt(Fee fee, PaymentProof proof, double amountPaid,
                                          double concession, double lateFee,
                                          String paymentMode, String transactionRef) throws Exception {
        Library library = fee.getLibrary();
        Student student = fee.getStudent();

        // Auto-generate receipt number: RCPT-YYYY-NNNNN
        long seq = receiptRepository.countByLibraryId(library.getId()) + 1;
        String receiptNo = String.format("RCPT-%d-%05d", LocalDate.now().getYear(), seq);

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

        FeeReceipt saved = receiptRepository.save(receipt);
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
