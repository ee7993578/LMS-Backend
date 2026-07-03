package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Model.Notification;
import com.learningJWT.LearningTemplate.Model.Student;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.NotificationDTO;
import com.learningJWT.LearningTemplate.Repository.StudentRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

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

    // ===================== Student: dashboard banner =====================

    /** Active (not-yet-expired) library-wide broadcast(s) to show as a banner on the student dashboard. */
    @GetMapping("/dashboard-banner")
    public ResponseEntity<?> getDashboardBanner() {
        try {
            List<NotificationDTO> list = notificationService.getMyDashboardBroadcasts()
                    .stream().map(this::toDTO).collect(Collectors.toList());
            return ResponseEntity.ok(list);
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    // ===================== Library Admin: send & manage =====================

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PostMapping("/libraryadmin/broadcast")
    public ResponseEntity<?> sendBroadcast(@RequestBody Map<String, String> body) {
        try {
            User admin = getLoggedInAdmin();
            if (admin.getLibrary() == null) throw new Exception("Admin is not linked to a library");
            String title = body.get("title");
            String message = body.get("message");
            if (title == null || title.isBlank() || message == null || message.isBlank())
                throw new Exception("Title and message are required");
            notificationService.sendToLibrary(admin.getLibrary(), title, message);
            return ResponseEntity.ok().build();
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PostMapping("/libraryadmin/student/{studentId}")
    public ResponseEntity<?> sendToStudent(@PathVariable Long studentId, @RequestBody Map<String, String> body) {
        try {
            User admin = getLoggedInAdmin();
            if (admin.getLibrary() == null) throw new Exception("Admin is not linked to a library");
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new Exception("Student not found"));
            if (student.getLibrary() == null || !student.getLibrary().getId().equals(admin.getLibrary().getId()))
                throw new Exception("Student does not belong to your library");
            if (student.getUser() == null) throw new Exception("Student has no linked login account");
            String title = body.get("title");
            String message = body.get("message");
            if (title == null || title.isBlank() || message == null || message.isBlank())
                throw new Exception("Title and message are required");
            notificationService.sendToStudent(admin.getLibrary(), student.getUser(), title, message);
            return ResponseEntity.ok().build();
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping("/libraryadmin/sent")
    public ResponseEntity<?> getSentHistory() {
        try {
            User admin = getLoggedInAdmin();
            if (admin.getLibrary() == null) throw new Exception("Admin is not linked to a library");
            return ResponseEntity.ok(notificationService.getSentHistory(admin.getLibrary().getId()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @DeleteMapping("/libraryadmin/broadcast/{groupId}")
    public ResponseEntity<?> deleteBroadcast(@PathVariable String groupId) {
        notificationService.deleteBroadcast(groupId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @DeleteMapping("/libraryadmin/{id}")
    public ResponseEntity<?> deleteIndividual(@PathVariable Long id) {
        notificationService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private User getLoggedInAdmin() throws Exception {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
        }
        throw new Exception("Not authenticated");
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId()).title(n.getTitle()).message(n.getMessage())
                .type(n.getType()).isRead(n.isRead()).isArchived(n.isArchived())
                .actionUrl(n.getActionUrl()).createdAt(n.getCreatedAt())
                .build();
    }
}
