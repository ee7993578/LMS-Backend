package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.Fee;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Paylod.DTO.FeeDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeeRepository extends JpaRepository<Fee,Long> {

    List<Fee> findByStudentIdAndLibraryId(Long studentId, Long libraryId);
    @Query("SELECT COALESCE(MAX(f.monthId), 0) FROM Fee f WHERE f.student.id = :studentId AND f.library.id = :libraryId")
    int findGreatestMonthId(@Param("studentId") Long studentId,
                            @Param("libraryId") Long libraryId);
    Fee findByStudentIdAndLibraryIdAndMonthId(Long studentId, Long libraryId, int monthId);
    List<Fee> findByLibraryId(Long libraryId);
    List<Fee>  findByMonthIdAndLibraryId(int monthId, Long libraryId);

    @Query("SELECT f FROM Fee f WHERE f.student.id = :studentId AND f.library.id = :libraryId " +
           "AND f.feeStatus <> com.learningJWT.LearningTemplate.Enum.FeeStatus.PAID " +
           "ORDER BY f.monthId ASC")
    List<Fee> findPendingFeesOrderByMonthAsc(@Param("studentId") Long studentId, @Param("libraryId") Long libraryId);
}
