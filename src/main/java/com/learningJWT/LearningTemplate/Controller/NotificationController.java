package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Model.Notification;
import com.learningJWT.LearningTemplate.Paylod.DTO.NotificationDTO;
import com.learningJWT.LearningTemplate.Services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getMyNotifications() {
        try {
            List<NotificationDTO> list = notificationService.getMyNotifications()
                    .stream().map(this::toDTO).collect(Collectors.toList());
            return ResponseEntity.ok(list);
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        try {
            return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        try { notificationService.markRead(id); return ResponseEntity.ok().build(); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllRead() {
        try { notificationService.markAllRead(); return ResponseEntity.ok().build(); }
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable Long id) {
        notificationService.archive(id);
        return ResponseEntity.ok().build();
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId()).title(n.getTitle()).message(n.getMessage())
                .type(n.getType()).isRead(n.isRead()).isArchived(n.isArchived())
                .actionUrl(n.getActionUrl()).createdAt(n.getCreatedAt())
                .build();
    }
}
