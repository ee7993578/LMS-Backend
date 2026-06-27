package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Enum.AnnouncementType;
import com.learningJWT.LearningTemplate.Model.*;
import com.learningJWT.LearningTemplate.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final LibraryRepository libraryRepository;

    public Announcement create(String title, String content, AnnouncementType type,
                                Long targetLibraryId, LocalDateTime expiresAt) {
        Library target = (targetLibraryId != null)
                ? libraryRepository.findById(targetLibraryId).orElse(null) : null;
        return announcementRepository.save(Announcement.builder()
                .title(title).content(content).type(type)
                .targetLibrary(target).active(true)
                .createdAt(LocalDateTime.now()).expiresAt(expiresAt)
                .build());
    }

    public List<Announcement> getActiveForLibrary(Long libraryId) {
        return announcementRepository.findActiveForLibrary(libraryId, LocalDateTime.now());
    }

    public List<Announcement> getAll() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    public void deactivate(Long id) {
        announcementRepository.findById(id).ifPresent(a -> {
            a.setActive(false);
            announcementRepository.save(a);
        });
    }

    public void delete(Long id) {
        announcementRepository.deleteById(id);
    }
}
