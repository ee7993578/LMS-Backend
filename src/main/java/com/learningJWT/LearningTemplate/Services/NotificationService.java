package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Enum.NotificationChannel;
import com.learningJWT.LearningTemplate.Enum.NotificationType;
import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Paylod.DTO.SentNotificationDTO;
import com.learningJWT.LearningTemplate.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    /** How long a library-wide broadcast stays pinned on every student's dashboard. */
    private static final long BROADCAST_DASHBOARD_HOURS = 24;

    @Async
    public void send(User user, Library library, NotificationType type,
                     String title, String message, String actionUrl) {
        Notification n = Notification.builder()
                .user(user).library(library).type(type)
                .channel(NotificationChannel.IN_APP)
                .title(title).message(message)
                .actionUrl(actionUrl)
                .isRead(false).isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(n);
    }

    @Async
    public void sendToLibraryAdmin(Library library, NotificationType type,
                                    String title, String message) {
        if (library != null && library.getAdmin() != null) {
            send(library.getAdmin(), library, type, title, message, null);
        }
    }

    public List<Notification> getMyNotifications() throws Exception {
        User user = getLoggedInUser();
        return notificationRepository
                .findByUserIdAndIsArchivedFalseOrderByCreatedAtDesc(user.getId());
    }

    public long getUnreadCount() throws Exception {
        User user = getLoggedInUser();
        return notificationRepository
                .countByUserIdAndIsReadFalseAndIsArchivedFalse(user.getId());
    }

    @Transactional
    public void markRead(Long id) throws Exception {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllRead() throws Exception {
        User user = getLoggedInUser();
        notificationRepository.markAllReadByUser(user.getId());
    }

    @Transactional
    public void archive(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setArchived(true);
            notificationRepository.save(n);
        });
    }

    private User getLoggedInUser() throws Exception {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
        }
        throw new Exception("Not authenticated");
    }

    // ===================== Library Admin: send / manage / delete =====================

    /**
     * Sends one message to every active student in the admin's library. One Notification
     * row is created per student (so per-student read/unread + the existing bell UI keep
     * working unchanged), but all rows share a broadcastGroupId so they can be managed and
     * deleted together as a single logical notification. Also gets a dashboardExpiresAt of
     * now+24h so it shows as a banner on every student dashboard for exactly one day.
     */
    @Transactional
    public void sendToLibrary(Library library, String title, String message) throws Exception {
        List<Student> students = studentRepository.findByLibrary(library);
        String groupId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dashboardExpiry = now.plusHours(BROADCAST_DASHBOARD_HOURS);

        List<Notification> batch = new ArrayList<>();
        for (Student s : students) {
            if (!s.isActive() || s.getUser() == null) continue;
            batch.add(Notification.builder()
                    .user(s.getUser()).library(library).type(NotificationType.ADMIN_MESSAGE)
                    .channel(NotificationChannel.IN_APP)
                    .title(title).message(message)
                    .isRead(false).isArchived(false)
                    .createdAt(now)
                    .broadcastGroupId(groupId)
                    .dashboardExpiresAt(dashboardExpiry)
                    .build());
        }
        if (batch.isEmpty()) throw new Exception("Library has no active students to notify");
        notificationRepository.saveAll(batch);
    }

    /** Sends one message to a single student. Shows in their bell icon only (no dashboard banner). */
    @Transactional
    public void sendToStudent(Library library, User studentUser, String title, String message) {
        Notification n = Notification.builder()
                .user(studentUser).library(library).type(NotificationType.ADMIN_MESSAGE)
                .channel(NotificationChannel.IN_APP)
                .title(title).message(message)
                .isRead(false).isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(n);
    }

    /** Still-active library-wide broadcast(s) for the logged-in student's dashboard banner. */
    public List<Notification> getMyDashboardBroadcasts() throws Exception {
        User user = getLoggedInUser();
        return notificationRepository.findActiveDashboardBroadcastsForUser(user.getId(), LocalDateTime.now());
    }

    /** Sent-history for the Library Admin management screen: broadcasts collapsed to one row + individual sends. */
    public List<SentNotificationDTO> getSentHistory(Long libraryId) {
        List<Notification> all = notificationRepository.findAdminSentForLibrary(libraryId);
        LocalDateTime now = LocalDateTime.now();

        Map<String, List<Notification>> byGroup = new LinkedHashMap<>();
        List<Notification> individuals = new ArrayList<>();
        for (Notification n : all) {
            if (n.getBroadcastGroupId() != null) {
                byGroup.computeIfAbsent(n.getBroadcastGroupId(), k -> new ArrayList<>()).add(n);
            } else {
                individuals.add(n);
            }
        }

        List<SentNotificationDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<Notification>> e : byGroup.entrySet()) {
            Notification first = e.getValue().get(0);
            result.add(SentNotificationDTO.builder()
                    .key(e.getKey()).title(first.getTitle()).message(first.getMessage())
                    .scope("LIBRARY").recipientCount(e.getValue().size())
                    .createdAt(first.getCreatedAt()).dashboardExpiresAt(first.getDashboardExpiresAt())
                    .dashboardActive(first.getDashboardExpiresAt() != null && first.getDashboardExpiresAt().isAfter(now))
                    .build());
        }
        for (Notification n : individuals) {
            result.add(SentNotificationDTO.builder()
                    .key(String.valueOf(n.getId())).title(n.getTitle()).message(n.getMessage())
                    .scope("STUDENT").studentName(n.getUser() != null ? n.getUser().getFullName() : "—")
                    .recipientCount(1).createdAt(n.getCreatedAt()).dashboardActive(false)
                    .build());
        }
        result.sort(Comparator.comparing(SentNotificationDTO::getCreatedAt).reversed());
        return result;
    }

    /** Deletes every copy of a library-wide broadcast — removes it from every student's bell + dashboard at once. */
    @Transactional
    public void deleteBroadcast(String groupId) {
        notificationRepository.deleteByBroadcastGroupId(groupId);
    }

    /** Deletes one individual (student-specific) notification. */
    @Transactional
    public void deleteById(Long id) {
        notificationRepository.deleteById(id);
    }
}
