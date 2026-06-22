package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibraryRepository extends JpaRepository<Library,Long> {

    List<Library> findByStatus(Status status);
}
