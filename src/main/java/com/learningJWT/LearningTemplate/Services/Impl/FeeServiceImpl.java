package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Enum.FeeStatus;
import com.learningJWT.LearningTemplate.Mapper.FeeMapper;
import com.learningJWT.LearningTemplate.Model.Fee;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeDTO;
import com.learningJWT.LearningTemplate.Paylod.Response.ApiResponse;
import com.learningJWT.LearningTemplate.Repository.FeeRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.FeeServices;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeServiceImpl implements FeeServices {

    private final FeeRepository feeRepository;
    private final UserRepository userRepository;
    private final PaymentRecordingService paymentRecordingService;

    private User getLoggedInAdmin() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new Exception("Logged-in user not found"));
        }
        throw new Exception("No authenticated user found");
    }

    @Override
    public FeeDTO createFee(Fee fee) throws Exception {
        return FeeMapper.toDTO(feeRepository.save(fee));
    }

    @Override
    public FeeDTO updateFee(Long studentId, FeeDTO dto) throws Exception {
        Long libraryId = getLoggedInAdmin().getLibrary().getId();
        int maxMonthId = findGreatestMonthId(studentId, libraryId);
        if (maxMonthId <= 0) {
            throw new Exception("No fee record found for this student");
        }
        Fee fee = feeRepository.findByStudentIdAndLibraryIdAndMonthId(studentId, libraryId, maxMonthId);
        if (fee == null) throw new Exception("Fee record not found");

        if (dto.getDueDate() != null) fee.setDueDate(dto.getDueDate());
        if (dto.getPayable() > 0) fee.setPayable(dto.getPayable());

        // This endpoint sets absolute values (not additive) — same behaviour as before.
        // We only track the DELTA so PaymentRecordingService knows how much NEW cash was
        // actually received in this edit, and generates a receipt for exactly that amount.
        double previousReceive = fee.getReceive();
        fee.setReceive(dto.getReceive());
        double delta = dto.getReceive() - previousReceive;

        // recordPayment() will always be called — even when delta <= 0 — so that
        // due-date/payable/concession/lateFee/status edits still persist. It only generates a
        // receipt + audit log + notification when delta > 0 (i.e. new money was actually recorded),
        // which is what fixes the "cash payment recorded manually produces no receipt" bug.
        return paymentRecordingService.recordPayment(
                fee, delta, dto.getConcession(), dto.getLateFee(), dto.getFeeStatus(),
                dto.getPaymentMode(), dto.getTransactionRef(), null, "ADMIN_MANUAL_EDIT");
    }

    @Override
    public List<FeeDTO> getFeeByStudentId(Long studentId, Long libraryId) throws Exception {
        List<Fee> fees = feeRepository.findByStudentIdAndLibraryId(studentId, libraryId);
        return fees.stream().map(FeeMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<FeeDTO> getFeeByLibraryId() throws Exception {
        User user = getLoggedInAdmin();
        List<Fee> fees = feeRepository.findByLibraryId(user.getLibrary().getId());
        return fees.stream().map(FeeMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public FeeDTO getFeeByMonthId(int monthId, Long studentId, Long libraryId) throws Exception {
        return FeeMapper.toDTO(feeRepository.findByStudentIdAndLibraryIdAndMonthId(studentId, libraryId, monthId));
    }

    @Override
    public int findGreatestMonthId(Long studentId, Long libraryId) throws Exception {
        try {
            return feeRepository.findGreatestMonthId(studentId, libraryId);
        } catch (Exception e) {
            throw new Exception("Error fetching fee for studentId=" + studentId + " libraryId=" + libraryId, e);
        }
    }

    @Override
    public List<FeeDTO> findByMonthIdLibraryId(int monthId) throws Exception {
        User user = getLoggedInAdmin();
        List<Fee> fees = feeRepository.findByMonthIdAndLibraryId(monthId, user.getLibrary().getId());
        return fees.stream().map(FeeMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public FeeDTO feeDeposit(Long studentId, FeeDTO feeDTO) throws Exception {
        return null;
    }

    @Override
    public ApiResponse FeeDelete(Long studentId, FeeDTO feeDTO) throws Exception {
        return null;
    }

    @Override
    public FeeDTO applyPayment(Long studentId, double amountReceived, double concession, double lateFee, FeeStatus forcedStatus) throws Exception {
        Long libraryId = getLoggedInAdmin().getLibrary().getId();

        List<Fee> pending = feeRepository.findPendingFeesOrderByMonthAsc(studentId, libraryId);
        Fee fee;
        if (!pending.isEmpty()) {
            fee = pending.get(0); // oldest outstanding month — pay dues in order
        } else {
            int maxMonthId = findGreatestMonthId(studentId, libraryId);
            if (maxMonthId <= 0) throw new Exception("No fee record found for this student");
            fee = feeRepository.findByStudentIdAndLibraryIdAndMonthId(studentId, libraryId, maxMonthId);
            if (fee == null) throw new Exception("Fee record not found");
        }

        // Additive: add this payment on top of whatever was already received/credited.
        fee.setReceive(fee.getReceive() + amountReceived);
        fee.setConcession(fee.getConcession() + concession);
        fee.setLateFee(fee.getLateFee() + lateFee);

        double balance = (fee.getPayable() + fee.getLateFee()) - (fee.getReceive() + fee.getConcession());
        fee.setBalance(Math.max(0, balance));

        if (forcedStatus != null) {
            fee.setFeeStatus(forcedStatus);
        } else if (fee.getBalance() <= 0) {
            fee.setFeeStatus(FeeStatus.PAID);
        } else if (fee.getReceive() > 0) {
            fee.setFeeStatus(FeeStatus.PARTIAL);
        } else {
            fee.setFeeStatus(FeeStatus.UNPAID);
        }

        if (fee.getFeeStatus() == FeeStatus.PAID) {
            fee.setPaymentDate(java.time.LocalDate.now());
        }

        return FeeMapper.toDTO(feeRepository.save(fee));
    }
}
