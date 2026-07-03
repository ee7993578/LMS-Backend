package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Enum.FeeStatus;
import com.learningJWT.LearningTemplate.Enum.NotificationType;
import com.learningJWT.LearningTemplate.Mapper.FeeMapper;
import com.learningJWT.LearningTemplate.Model.Fee;
import com.learningJWT.LearningTemplate.Model.PaymentProof;
import com.learningJWT.LearningTemplate.Model.StudentSubscription;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeDTO;
import com.learningJWT.LearningTemplate.Repository.FeeRepository;
import com.learningJWT.LearningTemplate.Services.FeeReceiptService;
import com.learningJWT.LearningTemplate.Services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single code path for "money moved on a Fee record". Whether the payment came from a
 * student's proof-verification flow or an admin's direct cash entry, it should always end
 * up here so that a receipt + audit log entry is guaranteed either way (fixes the bug where
 * manual admin fee edits silently skipped receipt generation).
 */
@Service
@RequiredArgsConstructor
public class PaymentRecordingService {

    private final FeeRepository feeRepository;
    private final FeeReceiptService feeReceiptService;
    private final FeeAuditLogService feeAuditLogService;
    private final NotificationService notificationService;

    /**
     * Records a payment against an existing Fee row and keeps the linked StudentSubscription's
     * paid/balance snapshot (used by the student "My Plan" card) in sync.
     *
     * @param amountDelta the NEW money actually received in this action (not the running total).
     *                     Pass 0 if this call is only adjusting concession/late fee/status with no
     *                     new cash received — in that case no receipt is generated.
     */
    @Transactional(rollbackFor = Exception.class)
    public FeeDTO recordPayment(Fee fee, double amountDelta, double concession, double lateFee,
                                 FeeStatus forcedStatus, String paymentMode, String transactionRef,
                                 PaymentProof proofOrNull, String source) throws Exception {

        fee.setConcession(concession);
        fee.setLateFee(lateFee);
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

        if (amountDelta > 0) {
            fee.setPaymentDate(java.time.LocalDate.now());
        }

        Fee saved = feeRepository.save(fee);

        // Keep the subscription cycle's paid/balance snapshot in sync (drives the student's "My Plan" card)
        StudentSubscription sub = saved.getSubscription();
        if (sub != null) {
            sub.setPaid(saved.getReceive());
            sub.setBalance(saved.getBalance());
        }

        if (amountDelta > 0) {
            // ALWAYS generate a receipt for real money received — cash or proof-verified alike.
            feeReceiptService.generateReceipt(saved, proofOrNull, amountDelta, concession, lateFee,
                    paymentMode != null ? paymentMode : "Cash", transactionRef);

            feeAuditLogService.log(saved, saved.getLibrary(), saved.getStudent(),
                    "RECORD_PAYMENT",
                    String.format("Source=%s, AmountReceived=%.2f, Concession=%.2f, LateFee=%.2f, Balance=%.2f",
                            source, amountDelta, concession, lateFee, saved.getBalance()));

            if (saved.getStudent() != null && saved.getStudent().getUser() != null) {
                notificationService.send(
                        saved.getStudent().getUser(), saved.getLibrary(),
                        NotificationType.PAYMENT_APPROVED,
                        "Payment Recorded",
                        String.format("A payment of \u20B9%.0f was recorded on your account. Balance: \u20B9%.0f",
                                amountDelta, saved.getBalance()),
                        "/student/receipts");
            }
        }

        return FeeMapper.toDTO(saved);
    }
}
