package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Enum.AllocationMode;
import com.learningJWT.LearningTemplate.Mapper.LibraryMapper;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/libraryadmin/settings")
@RequiredArgsConstructor
public class LibrarySettingsController {

    private final UserRepository userRepository;
    private final LibraryRepository libraryRepository;

    private User getLoggedInAdmin() throws Exception {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof UserDetails ud) {
            return userRepository.findByUsername(ud.getUsername())
                    .orElseThrow(() -> new Exception("User not found"));
        }
        throw new Exception("Not authenticated");
    }

    /** GET /api/libraryadmin/settings — returns library info including allocationMode */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @GetMapping
    public ResponseEntity<LibraryDTO> getMyLibrary() throws Exception {
        User admin = getLoggedInAdmin();
        return ResponseEntity.ok(LibraryMapper.toDTO(admin.getLibrary()));
    }

    /** PUT /api/libraryadmin/settings — update library info and allocation mode */
    @PreAuthorize("hasRole('LIBRARY_ADMIN')")
    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody LibraryDTO dto) throws Exception {
        User admin = getLoggedInAdmin();
        Library library = admin.getLibrary();

        if (dto.getName() != null) library.setName(dto.getName());
        if (dto.getAddress() != null) library.setAddress(dto.getAddress());
        if (dto.getEmail() != null) library.setEmail(dto.getEmail());
        if (dto.getPhone() != null) library.setPhone(dto.getPhone());
        if (dto.getWebsite() != null) library.setWebsite(dto.getWebsite());
        if (dto.getAllocationMode() != null) library.setAllocationMode(dto.getAllocationMode());
        library.setUpdatedAt(LocalDateTime.now());

        libraryRepository.save(library);
        return ResponseEntity.ok(LibraryMapper.toDTO(library));
    }
}
