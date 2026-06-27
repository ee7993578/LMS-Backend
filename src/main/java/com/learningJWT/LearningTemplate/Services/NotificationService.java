package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Enum.NotificationChannel;
import com.learningJWT.LearningTemplate.Enum.NotificationType;
import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

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
}
