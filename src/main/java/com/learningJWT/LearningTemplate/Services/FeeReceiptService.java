package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Model.Fee;
import com.learningJWT.LearningTemplate.Model.PaymentProof;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeReceiptDTO;

import java.util.List;

public interface FeeReceiptService {

    /** Generate and persist a receipt after payment verification */
    FeeReceiptDTO generateReceipt(Fee fee, PaymentProof proof, double amountPaid,
                                   double concession, double lateFee,
                                   String paymentMode, String transactionRef) throws Exception;

    /** Get all receipts for a student (student portal) */
    List<FeeReceiptDTO> getMyReceipts() throws Exception;

    /** Get receipt by ID */
    FeeReceiptDTO getReceiptById(Long id) throws Exception;

    /** Get all receipts for library (admin) */
    List<FeeReceiptDTO> getLibraryReceipts() throws Exception;

    /** Get receipts for a specific student (admin) */
    List<FeeReceiptDTO> getStudentReceipts(Long studentId) throws Exception;
}
