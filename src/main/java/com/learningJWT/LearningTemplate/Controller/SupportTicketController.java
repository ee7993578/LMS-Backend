package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Enum.TicketStatus;
import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Paylod.DTO.*;
import com.learningJWT.LearningTemplate.Services.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService ticketService;

    // Library Admin endpoints
    @PostMapping("/api/libraryadmin/tickets")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            SupportTicket t = ticketService.create(
                    body.get("subject"), body.get("description"), body.get("priority"));
            return ResponseEntity.ok(toDTO(t, true));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping("/api/libraryadmin/tickets")
    public ResponseEntity<?> myTickets() {
        try {
            return ResponseEntity.ok(ticketService.getMyTickets().stream()
                    .map(t -> toDTO(t, false)).collect(Collectors.toList()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping("/api/libraryadmin/tickets/{id}")
    public ResponseEntity<?> getTicket(@PathVariable Long id) {
        try { return ResponseEntity.ok(toDTO(ticketService.getById(id), true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/api/libraryadmin/tickets/{id}/reply")
    public ResponseEntity<?> addReply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            TicketReply r = ticketService.addReply(id, body.get("message"));
            return ResponseEntity.ok(toReplyDTO(r));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    // Super Admin endpoints
    @GetMapping("/api/superadmin/tickets")
    public ResponseEntity<?> allTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets().stream()
                .map(t -> toDTO(t, false)).collect(Collectors.toList()));
    }

    @GetMapping("/api/superadmin/tickets/stats")
    public ResponseEntity<?> stats() { return ResponseEntity.ok(ticketService.getStats()); }

    @GetMapping("/api/superadmin/tickets/{id}")
    public ResponseEntity<?> adminGetTicket(@PathVariable Long id) {
        try { return ResponseEntity.ok(toDTO(ticketService.getById(id), true)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PutMapping("/api/superadmin/tickets/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            TicketStatus status = TicketStatus.valueOf(body.get("status").toUpperCase());
            return ResponseEntity.ok(toDTO(ticketService.updateStatus(id, status), false));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/api/superadmin/tickets/{id}/reply")
    public ResponseEntity<?> adminReply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            TicketReply r = ticketService.addReply(id, body.get("message"));
            return ResponseEntity.ok(toReplyDTO(r));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    private SupportTicketDTO toDTO(SupportTicket t, boolean withReplies) {
        List<TicketReplyDTO> replies = withReplies
                ? t.getReplies().stream().map(this::toReplyDTO).collect(Collectors.toList())
                : List.of();
        return SupportTicketDTO.builder()
                .id(t.getId()).subject(t.getSubject()).description(t.getDescription())
                .status(t.getStatus()).priority(t.getPriority())
                .libraryName(t.getLibrary() != null ? t.getLibrary().getName() : null)
                .createdByName(t.getCreatedBy() != null ? t.getCreatedBy().getUsername() : null)
                .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .resolvedAt(t.getResolvedAt()).replies(replies)
                .build();
    }

    private TicketReplyDTO toReplyDTO(TicketReply r) {
        return TicketReplyDTO.builder()
                .id(r.getId()).message(r.getMessage())
                .repliedBy(r.getRepliedBy() != null ? r.getRepliedBy().getUsername() : null)
                .isAdminReply(r.isAdminReply()).createdAt(r.getCreatedAt())
                .build();
    }
}
