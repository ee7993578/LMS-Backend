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
        if (maxMonthId > 0) {
            Fee fee = feeRepository.findByStudentIdAndLibraryIdAndMonthId(studentId, libraryId, maxMonthId);
            if (fee == null) throw new Exception("Fee record not found");

            if (dto.getDueDate() != null) fee.setDueDate(dto.getDueDate());
            if (dto.getPayable() > 0) fee.setPayable(dto.getPayable());
            fee.setReceive(dto.getReceive());
            fee.setConcession(dto.getConcession());
            fee.setLateFee(dto.getLateFee());
            double balance = (fee.getPayable() + dto.getLateFee()) - (dto.getReceive() + dto.getConcession());
            fee.setBalance(Math.max(0, balance));

            // Auto-set status based on balance
            if (dto.getFeeStatus() != null) {
                fee.setFeeStatus(dto.getFeeStatus());
            } else {
                if (fee.getBalance() <= 0) {
                    fee.setFeeStatus(FeeStatus.PAID);
                } else if (dto.getReceive() > 0) {
                    fee.setFeeStatus(FeeStatus.PARTIAL);
                } else {
                    fee.setFeeStatus(FeeStatus.UNPAID);
                }
            }

            if (fee.getFeeStatus() == FeeStatus.PAID && dto.getReceive() > 0) {
                fee.setPaymentDate(java.time.LocalDate.now());
            }

            return FeeMapper.toDTO(feeRepository.save(fee));
        } else {
            throw new Exception("No fee record found for this student");
        }
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
}
