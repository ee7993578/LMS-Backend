package com.learningJWT.LearningTemplate.Services.Impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    /**
     * Saves the file under {uploadDir}/{subFolder}/ with a random name, keeping the original extension.
     * Returns the relative path (subFolder/filename.ext) to store in the DB.
     */
    public String store(MultipartFile file, String subFolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File too large (max 5MB)");
        }

        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Only image files are allowed (png, jpg, jpeg, webp, gif)");
        }

        Path dir = Paths.get(uploadDir, subFolder);
        Files.createDirectories(dir);

        String filename = UUID.randomUUID() + "." + ext;
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return subFolder + "/" + filename;
    }

    /** Deletes a previously stored file given its relative path. Silently ignores if missing. */
    public void delete(String relativePath) {
        if (relativePath == null) return;
        try {
            Files.deleteIfExists(Paths.get(uploadDir, relativePath));
        } catch (IOException ignored) {
        }
    }
}
