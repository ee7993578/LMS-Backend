package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryDTO;
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
}
