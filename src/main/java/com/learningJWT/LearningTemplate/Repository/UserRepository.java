package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    void deleteByLibraryId(Long username);

    // Eagerly fetches the Library along with User so LibraryAccessFilter never hits
    // LazyInitializationException (filter runs outside a transactional context).
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.library WHERE u.username = :username")
    Optional<User> findByUsernameWithLibrary(@Param("username") String username);

}
