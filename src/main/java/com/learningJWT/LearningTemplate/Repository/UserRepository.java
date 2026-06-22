package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    void deleteByLibraryId(Long username);

}
