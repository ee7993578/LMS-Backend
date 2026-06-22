package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Enum.Status;
import com.learningJWT.LearningTemplate.Enum.UserRole;
import com.learningJWT.LearningTemplate.Mapper.LibraryMapper;
import com.learningJWT.LearningTemplate.Model.Library;
import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Repository.LibraryPlanRepository;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final LibraryRepository libraryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LibraryPlanRepository libraryPlanRepository;

    @Override
    public LibraryDTO createLibrary(LibraryDTO dto) throws Exception {

        if (dto.getAdminUsername() == null || dto.getAdminUsername().isBlank()) {
            throw new Exception("Admin username is required!");
        }

        if (userRepository.existsByUsername(dto.getAdminUsername())) {
            throw new Exception("Admin username already exists!");
        }

        Library library = LibraryMapper.toEntity(dto);
        library.setStatus(Status.PENDING);
        library.setCreatedAt(LocalDateTime.now());
        library.setUpdatedAt(LocalDateTime.now());

        // Plan is optional during registration
        if (dto.getLibraryPlanId() != null) {
            LibraryPlan plan = libraryPlanRepository.findById(dto.getLibraryPlanId())
                    .orElseThrow(() -> new Exception("Library plan not found"));
            library.setLibraryPlan(plan);
        }

        libraryRepository.save(library);

        User admin = new User();
        admin.setUsername(dto.getAdminUsername());
        admin.setFullName(dto.getAdminFullName());
        admin.setPhone(dto.getAdminPhone());
        admin.setPassword(passwordEncoder.encode(dto.getAdminPassword()));
        admin.setRole(UserRole.ROLE_LIBRARY_ADMIN);
        admin.setLibrary(library);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());

        userRepository.save(admin);

        library.setAdmin(admin);
        libraryRepository.save(library);

        return LibraryMapper.toDTO(library);
    }

    @Override
    public LibraryDTO updateLibrary(Long libraryId, LibraryDTO dto) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));

        library.setName(dto.getName());
        library.setAddress(dto.getAddress());
        library.setEmail(dto.getEmail());
        library.setPhone(dto.getPhone());
        library.setWebsite(dto.getWebsite());
        if (dto.getStatus() != null) {
            library.setStatus(dto.getStatus());
        }
        library.setUpdatedAt(LocalDateTime.now());

        if (dto.getLibraryPlanId() != null) {
            LibraryPlan plan = libraryPlanRepository.findById(dto.getLibraryPlanId())
                    .orElseThrow(() -> new Exception("Library plan not found"));
            library.setLibraryPlan(plan);
        }

        User admin = library.getAdmin();
        if (admin != null) {
            if (dto.getAdminFullName() != null) admin.setFullName(dto.getAdminFullName());
            if (dto.getAdminPhone() != null) admin.setPhone(dto.getAdminPhone());
            if (dto.getAdminPassword() != null && !dto.getAdminPassword().isEmpty()) {
                admin.setPassword(passwordEncoder.encode(dto.getAdminPassword()));
            }
            admin.setUpdatedAt(LocalDateTime.now());
            userRepository.save(admin);
        }

        libraryRepository.save(library);
        return LibraryMapper.toDTO(library);
    }

    @Override
    public void deleteLibrary(Long libraryId) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));

        User admin = library.getAdmin();
        if (admin != null) {
            userRepository.delete(admin);
        }

        libraryRepository.delete(library);
    }

    @Override
    public List<LibraryDTO> getAllLibraries() {
        List<Library> libraries = libraryRepository.findAll();
        return libraries.stream().map(LibraryMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public LibraryDTO findById(Long libraryId) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));
        return LibraryMapper.toDTO(library);
    }

    @Override
    public List<LibraryDTO> findByStatus(String status) throws Exception {
        List<Library> libraries = libraryRepository.findByStatus(Status.valueOf(status));
        return libraries.stream().map(LibraryMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public LibraryDTO changeLibraryStatus(Long libraryId, String status) throws Exception {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new Exception("Library not found"));
        library.setStatus(Status.valueOf(status));
        libraryRepository.save(library);
        return LibraryMapper.toDTO(library);
    }

    @Override
    public LibraryDTO assignPlanToLibrary(Long libraryId, Long planId) {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new RuntimeException("Library not found"));
        LibraryPlan plan = libraryPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        library.setLibraryPlan(plan);
        Library updated = libraryRepository.save(library);
        return LibraryMapper.toDTO(updated);
    }
}
