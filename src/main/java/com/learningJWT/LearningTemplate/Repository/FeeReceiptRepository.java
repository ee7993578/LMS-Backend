package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.FeeReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeeReceiptRepository extends JpaRepository<FeeReceipt, Long> {

    List<FeeReceipt> findByStudentIdOrderByGeneratedAtDesc(Long studentId);

    List<FeeReceipt> findByLibraryIdOrderByGeneratedAtDesc(Long libraryId);

    @Query("SELECT COALESCE(MAX(r.id), 0) FROM FeeReceipt r WHERE r.library.id = :libraryId")
    long countByLibraryId(@Param("libraryId") Long libraryId);

    Optional<FeeReceipt> findByReceiptNumber(String receiptNumber);

    List<FeeReceipt> findByFeeIdOrderByGeneratedAtDesc(Long feeId);
}
