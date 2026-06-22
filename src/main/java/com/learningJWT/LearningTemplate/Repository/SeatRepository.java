package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.Seat;
import com.learningJWT.LearningTemplate.Paylod.DTO.SeatDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByLibraryIdAndSeatNameContainingIgnoreCase(Long libraryId, String keyword);

    List<Seat> findByLibraryId(Long libraryId);
}
