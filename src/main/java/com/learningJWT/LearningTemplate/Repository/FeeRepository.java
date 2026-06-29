package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Enum.FeeStatus;
import com.learningJWT.LearningTemplate.Model.Fee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeeRepository extends JpaRepository<Fee, Long> {

    List<Fee> findByStudentIdAndLibraryId(Long studentId, Long libraryId);

    @Query("SELECT COALESCE(MAX(f.monthId), 0) FROM Fee f WHERE f.student.id = :studentId AND f.library.id = :libraryId")
    int findGreatestMonthId(@Param("studentId") Long studentId, @Param("libraryId") Long libraryId);

    Fee findByStudentIdAndLibraryIdAndMonthId(Long studentId, Long libraryId, int monthId);

    // Non-paginated (small sets — ledger, verify flow)
    List<Fee> findByLibraryId(Long libraryId);
    List<Fee> findByMonthIdAndLibraryId(int monthId, Long libraryId);

    // Paginated versions for reports
    Page<Fee> findByLibraryId(Long libraryId, Pageable pageable);

    @Query("SELECT f FROM Fee f WHERE f.library.id = :libId AND f.feeStatus = :status")
    Page<Fee> findByLibraryIdAndStatus(@Param("libId") Long libraryId,
                                        @Param("status") FeeStatus status, Pageable pageable);

    @Query("SELECT f FROM Fee f WHERE f.library.id = :libId AND f.monthId = :monthId")
    Page<Fee> findByLibraryIdAndMonthId(@Param("libId") Long libraryId,
                                         @Param("monthId") int monthId, Pageable pageable);

    @Query("SELECT f FROM Fee f WHERE f.library.id = :libId " +
           "AND (:search IS NULL OR LOWER(f.student.fullName) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR f.student.phone LIKE CONCAT('%',:search,'%') " +
           "OR f.student.admissionNumber LIKE CONCAT('%',:search,'%'))")
    Page<Fee> searchByLibrary(@Param("libId") Long libraryId,
                               @Param("search") String search, Pageable pageable);

    @Query("SELECT f FROM Fee f WHERE f.student.id = :studentId AND f.library.id = :libraryId " +
           "AND f.feeStatus <> com.learningJWT.LearningTemplate.Enum.FeeStatus.PAID " +
           "ORDER BY f.monthId ASC")
    List<Fee> findPendingFeesOrderByMonthAsc(@Param("studentId") Long studentId,
                                              @Param("libraryId") Long libraryId);

    // Composite index hint queries
    @Query("SELECT f FROM Fee f WHERE f.library.id = :libId " +
           "AND f.feeStatus != com.learningJWT.LearningTemplate.Enum.FeeStatus.PAID " +
           "AND f.balance > 0 ORDER BY f.balance DESC")
    List<Fee> findPendingFeesByLibrary(@Param("libId") Long libraryId);

    @Query("SELECT f FROM Fee f WHERE f.library.id = :libId " +
           "AND f.monthId = :monthId " +
           "AND (:status IS NULL OR f.feeStatus = :status)")
    List<Fee> findByLibraryIdAndMonthIdAndStatus(@Param("libId") Long libraryId,
                                                  @Param("monthId") int monthId,
                                                  @Param("status") FeeStatus status);
}
