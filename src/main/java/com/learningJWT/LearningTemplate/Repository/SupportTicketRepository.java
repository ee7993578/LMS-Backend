package com.learningJWT.LearningTemplate.Repository;

import com.learningJWT.LearningTemplate.Enum.TicketStatus;
import com.learningJWT.LearningTemplate.Model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByLibraryIdOrderByCreatedAtDesc(Long libraryId);
    List<SupportTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    long countByStatus(TicketStatus status);
}
