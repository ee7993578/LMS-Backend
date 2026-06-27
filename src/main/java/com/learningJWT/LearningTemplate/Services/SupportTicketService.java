package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Enum.TicketStatus;
import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketReplyRepository replyRepository;
    private final UserRepository userRepository;

    public SupportTicket create(String subject, String description, String priority) throws Exception {
        User user = getLoggedIn();
        SupportTicket t = SupportTicket.builder()
                .library(user.getLibrary())
                .createdBy(user)
                .subject(subject)
                .description(description)
                .priority(priority != null ? priority : "MEDIUM")
                .status(TicketStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return ticketRepository.save(t);
    }

    public TicketReply addReply(Long ticketId, String message) throws Exception {
        User user = getLoggedIn();
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new Exception("Ticket not found"));
        boolean isAdmin = user.getRole().name().contains("SUPERADMIN");
        TicketReply reply = TicketReply.builder()
                .ticket(ticket).repliedBy(user)
                .message(message).isAdminReply(isAdmin)
                .createdAt(LocalDateTime.now()).build();
        if (isAdmin && ticket.getStatus() == TicketStatus.OPEN)
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        return replyRepository.save(reply);
    }

    public SupportTicket updateStatus(Long ticketId, TicketStatus status) throws Exception {
        SupportTicket t = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new Exception("Ticket not found"));
        t.setStatus(status);
        t.setUpdatedAt(LocalDateTime.now());
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED)
            t.setResolvedAt(LocalDateTime.now());
        return ticketRepository.save(t);
    }

    public List<SupportTicket> getMyTickets() throws Exception {
        User user = getLoggedIn();
        return ticketRepository.findByLibraryIdOrderByCreatedAtDesc(user.getLibrary().getId());
    }

    public List<SupportTicket> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    public SupportTicket getById(Long id) throws Exception {
        return ticketRepository.findById(id).orElseThrow(() -> new Exception("Ticket not found"));
    }

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (TicketStatus s : TicketStatus.values())
            stats.put(s.name(), ticketRepository.countByStatus(s));
        return stats;
    }

    private User getLoggedIn() throws Exception {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof UserDetails ud)
            return userRepository.findByUsername(ud.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
        throw new Exception("Not authenticated");
    }
}
