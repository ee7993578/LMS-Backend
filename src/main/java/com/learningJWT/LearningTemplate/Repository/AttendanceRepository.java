package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Attendance findByStudentIdAndAttendanceDate(Long studentId, LocalDate attendanceDate);

    List<Attendance> findByStudentIdAndAttendanceDateBetween(Long studentId,
            LocalDate startDate, LocalDate endDate);

    List<Attendance> findByLibraryIdAndAttendanceDate(Long libraryId, LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.library.id = :libraryId " +
           "AND a.attendanceDate BETWEEN :start AND :end " +
           "ORDER BY a.attendanceDate ASC")
    List<Attendance> findByLibraryIdAndDateRange(@Param("libraryId") Long libraryId,
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.library.id = :libraryId " +
           "AND a.attendanceDate = :date")
    long countByLibraryIdAndDate(@Param("libraryId") Long libraryId,
            @Param("date") LocalDate date);

    @Query("SELECT a.attendanceDate, COUNT(a) FROM Attendance a " +
           "WHERE a.library.id = :libraryId " +
           "AND a.attendanceDate BETWEEN :start AND :end " +
           "GROUP BY a.attendanceDate ORDER BY a.attendanceDate ASC")
    List<Object[]> countByLibraryIdGroupedByDate(@Param("libraryId") Long libraryId,
            @Param("start") LocalDate start, @Param("end") LocalDate end);
}
