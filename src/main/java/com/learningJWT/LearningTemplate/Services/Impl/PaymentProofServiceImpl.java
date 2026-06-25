package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Enum.ProofStatus;
import com.learningJWT.LearningTemplate.Mapper.PaymentProofMapper;
import com.learningJWT.LearningTemplate.Model.PaymentProof;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.PaymentProofDTO;
import com.learningJWT.LearningTemplate.Repository.PaymentProofRepository;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.FeeServices;
import com.learningJWT.LearningTemplate.Services.PaymentProofService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentProofServiceImpl implements PaymentProofService {

    private final PaymentProofRepository paymentProofRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FileStorageService fileStorageService;
    private final FeeServices feeServices;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private User getLoggedInUser() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new Exception("Logged-in user not found"));
        }
        throw new Exception("No authenticated user found");
    }

    private Student getLoggedInStudent() throws Exception {
        User user = getLoggedInUser();
        Student student = studentRepository.findByUserId(user.getId());
        if (student == null) throw new Exception("Student record not found");
        return student;
    }

    @Override
    public PaymentProofDTO submitProof(MultipartFile screenshotFile, String description, Double amountClaimed) throws Exception {
        Student student = getLoggedInStudent();
        if (student.getLibrary() == null) throw new Exception("Student is not linked to a library");

        boolean hasFile = screenshotFile != null && !screenshotFile.isEmpty();
        boolean hasDescription = description != null && !description.isBlank();
        if (!hasFile && !hasDescription) {
            throw new Exception("Please attach a screenshot or add a description");
        }

        PaymentProof.PaymentProofBuilder builder = PaymentProof.builder()
                .student(student)
                .library(student.getLibrary())
                .description(description)
                .amountClaimed(amountClaimed)
                .status(ProofStatus.PENDING)
                .submittedAt(LocalDateTime.now());

        if (hasFile) {
            builder.screenshotPath(fileStorageService.store(screenshotFile, "proof"));
        }

        PaymentProof saved = paymentProofRepository.save(builder.build());
        return PaymentProofMapper.toDTO(saved, baseUrl);
    }

    @Override
    public List<PaymentProofDTO> getMyProofs() throws Exception {
        Student student = getLoggedInStudent();
        return paymentProofRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId())
                .stream().map(p -> PaymentProofMapper.toDTO(p, baseUrl)).collect(Collectors.toList());
    }

    @Override
    public List<PaymentProofDTO> getLibraryProofs() throws Exception {
        User admin = getLoggedInUser();
        return paymentProofRepository.findByLibraryIdOrderBySubmittedAtDesc(admin.getLibrary().getId())
                .stream().map(p -> PaymentProofMapper.toDTO(p, baseUrl)).collect(Collectors.toList());
    }

    @Override
    public List<PaymentProofDTO> getPendingProofs() throws Exception {
        User admin = getLoggedInUser();
        return paymentProofRepository.findByLibraryIdAndStatusOrderBySubmittedAtDesc(admin.getLibrary().getId(), ProofStatus.PENDING)
                .stream().map(p -> PaymentProofMapper.toDTO(p, baseUrl)).collect(Collectors.toList());
    }

    @Override
    public FeeDTO verifyProof(Long proofId, FeeDTO feeDTO) throws Exception {
        PaymentProof proof = paymentProofRepository.findById(proofId)
                .orElseThrow(() -> new Exception("Payment proof not found"));

        User admin = getLoggedInUser();
        if (admin.getLibrary() == null || !admin.getLibrary().getId().equals(proof.getLibrary().getId())) {
            throw new Exception("Not authorized to verify this proof");
        }

        // Additive update: adds this payment on top of whatever the student already paid,
        // applied to the oldest outstanding month — does not overwrite prior deposits.
        FeeDTO updatedFee = feeServices.applyPayment(
                proof.getStudent().getId(),
                feeDTO.getReceive(),
                feeDTO.getConcession(),
                feeDTO.getLateFee(),
                feeDTO.getFeeStatus()
        );

        proof.setStatus(ProofStatus.VERIFIED);
        proof.setVerifiedAt(LocalDateTime.now());
        paymentProofRepository.save(proof);

        return updatedFee;
    }

    @Override
    public PaymentProofDTO rejectProof(Long proofId, String adminNote) throws Exception {
        PaymentProof proof = paymentProofRepository.findById(proofId)
                .orElseThrow(() -> new Exception("Payment proof not found"));

        User admin = getLoggedInUser();
        if (admin.getLibrary() == null || !admin.getLibrary().getId().equals(proof.getLibrary().getId())) {
            throw new Exception("Not authorized to reject this proof");
        }

        proof.setStatus(ProofStatus.REJECTED);
        proof.setVerifiedAt(LocalDateTime.now());
        proof.setAdminNote(adminNote);
        PaymentProof saved = paymentProofRepository.save(proof);
        return PaymentProofMapper.toDTO(saved, baseUrl);
    }
}
