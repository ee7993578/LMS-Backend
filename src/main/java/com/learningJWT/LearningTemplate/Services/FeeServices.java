package com.learningJWT.LearningTemplate.Services;

import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.learningJWT.LearningTemplate.Model.Fee;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeDTO;
import com.learningJWT.LearningTemplate.Paylod.Response.ApiResponse;

import java.util.List;

public interface FeeServices {

    FeeDTO createFee(Fee fee) throws Exception;
    FeeDTO updateFee(Long studentId,FeeDTO dto) throws Exception;
    List<FeeDTO> getFeeByStudentId(Long studentId,Long libraryId) throws Exception;
    List<FeeDTO> getFeeByLibraryId() throws Exception;
    FeeDTO getFeeByMonthId(int monthId, Long studentId, Long libraryId) throws Exception;
    int findGreatestMonthId(Long studentId, Long libraryId) throws Exception;
    List<FeeDTO> findByMonthIdLibraryId(int monthId) throws Exception;
    FeeDTO feeDeposit(Long studentId, FeeDTO feeDTO) throws Exception;
    ApiResponse FeeDelete(Long studentId, FeeDTO feeDTO) throws Exception;
}
