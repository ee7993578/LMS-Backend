package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryUsageDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.SuperAdminDashboardDTO;
import com.learningJWT.LearningTemplate.Model.Library;

import java.util.List;

public interface SuperAdminService {
    LibraryDTO createLibrary(LibraryDTO dto) throws Exception;
    LibraryDTO updateLibrary(Long libraryId, LibraryDTO dto) throws Exception;
    void deleteLibrary(Long libraryId) throws Exception;
    List<LibraryDTO> getAllLibraries();
    LibraryDTO findById(Long libraryId) throws Exception;
    List<LibraryDTO> findByStatus(String  status) throws Exception;
    LibraryDTO changeLibraryStatus(Long libraryId, String status) throws Exception;
    LibraryDTO assignPlanToLibrary(Long libraryId, Long planId) throws Exception;

    // ===== New: dashboard, lifecycle actions, usage =====
    SuperAdminDashboardDTO getDashboardStats() throws Exception;
    LibraryDTO activateLibrary(Long libraryId) throws Exception;
    LibraryDTO suspendLibrary(Long libraryId) throws Exception;
    LibraryDTO restoreLibrary(Long libraryId) throws Exception;
    LibraryDTO renewSubscription(Long libraryId, Integer days) throws Exception;
    LibraryDTO upgradePlan(Long libraryId, Long newPlanId) throws Exception;
    LibraryDTO downgradePlan(Long libraryId, Long newPlanId) throws Exception;
    LibraryUsageDTO getLibraryUsage(Long libraryId) throws Exception;
    List<LibraryUsageDTO> getAllLibraryUsage() throws Exception;
}
