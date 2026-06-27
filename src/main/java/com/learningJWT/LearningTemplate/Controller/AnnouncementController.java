package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Enum.AnnouncementType;
import com.learningJWT.LearningTemplate.Model.Announcement;
import com.learningJWT.LearningTemplate.Paylod.DTO.AnnouncementDTO;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final UserRepository userRepository;

    @GetMapping("/api/libraryadmin/announcements")
    public ResponseEntity<?> getForLibrary() {
        try {
            Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = (p instanceof UserDetails ud) ? ud.getUsername() : null;
            var user = userRepository.findByUsername(username).orElseThrow();
            Long libId = user.getLibrary() != null ? user.getLibrary().getId() : -1L;
            return ResponseEntity.ok(announcementService.getActiveForLibrary(libId)
                    .stream().map(this::toDTO).collect(Collectors.toList()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @GetMapping("/api/student/announcements")
    public ResponseEntity<?> getForStudent() {
        try {
            Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = (p instanceof UserDetails ud) ? ud.getUsername() : null;
            var user = userRepository.findByUsername(username).orElseThrow();
            Long libId = user.getLibrary() != null ? user.getLibrary().getId() : -1L;
            return ResponseEntity.ok(announcementService.getActiveForLibrary(libId)
                    .stream().map(this::toDTO).collect(Collectors.toList()));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    // Super Admin
    @GetMapping("/api/superadmin/announcements")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(announcementService.getAll()
                .stream().map(this::toDTO).collect(Collectors.toList()));
    }

    @PostMapping("/api/superadmin/announcements")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            AnnouncementType type = AnnouncementType.valueOf(
                    body.getOrDefault("type", "GENERAL").toString().toUpperCase());
            Long targetLibId = body.get("targetLibraryId") != null
                    ? Long.parseLong(body.get("targetLibraryId").toString()) : null;
            LocalDateTime expires = body.get("expiresAt") != null
                    ? LocalDateTime.parse(body.get("expiresAt").toString()) : null;
            Announcement a = announcementService.create(
                    body.get("title").toString(), body.get("content").toString(),
                    type, targetLibId, expires);
            return ResponseEntity.ok(toDTO(a));
        } catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @DeleteMapping("/api/superadmin/announcements/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/superadmin/announcements/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        announcementService.deactivate(id);
        return ResponseEntity.ok().build();
    }

    private AnnouncementDTO toDTO(Announcement a) {
        return AnnouncementDTO.builder()
                .id(a.getId()).title(a.getTitle()).content(a.getContent())
                .type(a.getType()).active(a.isActive())
                .targetLibraryId(a.getTargetLibrary() != null ? a.getTargetLibrary().getId() : null)
                .targetLibraryName(a.getTargetLibrary() != null ? a.getTargetLibrary().getName() : "All Libraries")
                .createdAt(a.getCreatedAt()).expiresAt(a.getExpiresAt())
                .build();
    }
}
