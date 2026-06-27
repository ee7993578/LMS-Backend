package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByLibrary(Library library);
    Student findByUserId(Long studentId) throws Exception;
    Optional<Student> findByLibraryIdAndSeatIdAndPlanId(Long libraryId, Long seatId, Long planId);
    int countByLibraryId(Long libraryId);
    long countByLibraryIdAndActiveTrue(Long libraryId);

    // Self-registration
    List<Student> findByLibraryIdAndRegistrationStatus(Long libraryId, String status);
    long countByLibraryIdAndRegistrationStatus(Long libraryId, String status);
    boolean existsByPhoneAndLibraryId(String phone, Long libraryId);
    boolean existsByEmailAndLibraryId(String email, Long libraryId);

    @Query("SELECT s FROM Student s WHERE s.library.id = :libId AND s.registrationStatus = 'PENDING_APPROVAL' ORDER BY s.createdAt DESC")
    List<Student> findPendingByLibrary(@Param("libId") Long libraryId);
}
