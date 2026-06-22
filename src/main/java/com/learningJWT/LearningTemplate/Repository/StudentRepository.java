package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByLibrary(Library library);
    Student findByUserId(Long studentId) throws  Exception;
    Optional<Student> findByLibraryIdAndSeatIdAndPlanId(Long libraryId, Long seatId, Long planId);
    int countByLibraryId(Long libraryId);

}
