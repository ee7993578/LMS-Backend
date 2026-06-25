package com.learningJWT.LearningTemplate.Services.Impl;

import com.learningJWT.LearningTemplate.Mapper.LibraryPlanMapper;
import com.learningJWT.LearningTemplate.Model.LibraryPlan;
import com.learningJWT.LearningTemplate.Model.Plan;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.LibraryPlanDTO;
import com.learningJWT.LearningTemplate.Paylod.Response.ApiResponse;
import com.learningJWT.LearningTemplate.Repository.LibraryPlanRepository;
import com.learningJWT.LearningTemplate.Repository.LibraryRepository;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.LibraryPlanServices;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LibraryPlanServiceImpl implements  LibraryPlanServices{
    private final UserRepository userRepository;
    private final LibraryPlanRepository libraryPlanRepository;
    private final LibraryRepository libraryRepository;

    @Override
    public LibraryPlanDTO createPlan(  LibraryPlanDTO libraryPlanDTO) throws Exception {

            LibraryPlan libraryPlan = new LibraryPlan();
            libraryPlan.setPlanName(libraryPlanDTO.getPlanName());
            libraryPlan.setPlanPrice(libraryPlanDTO.getPlanPrice());
            libraryPlan.setBufferStudent(libraryPlanDTO.getBufferStudent());
            libraryPlan.setNoOfStudent(libraryPlanDTO.getNoOfStudent());
            libraryPlan.setPlanOrder(libraryPlanDTO.getPlanOrder());
            libraryPlan.setNoOfDays(libraryPlanDTO.getNoOfDays());
            libraryPlan.setDescription(libraryPlanDTO.getDescription());
            libraryPlan.setIsActive(libraryPlanDTO.getIsActive() != null ? libraryPlanDTO.getIsActive() : true);
            libraryPlan.setGracePeriodDays(libraryPlanDTO.getGracePeriodDays() != null ? libraryPlanDTO.getGracePeriodDays() : 3);
            LibraryPlan saved = libraryPlanRepository.save(libraryPlan);
            return LibraryPlanMapper.toDto(saved);


    }

    @Override
    public LibraryPlanDTO updatePlan( Long id, LibraryPlanDTO libraryPlanDTO) throws Exception {

        LibraryPlan plan =  libraryPlanRepository.findById(id).orElseThrow(() -> new Exception("Plan not found"));
        plan.setBufferStudent(libraryPlanDTO.getBufferStudent());
        plan.setNoOfStudent(libraryPlanDTO.getNoOfStudent());
        plan.setPlanOrder(libraryPlanDTO.getPlanOrder());
        plan.setPlanPrice(libraryPlanDTO.getPlanPrice());
        plan.setNoOfDays(libraryPlanDTO.getNoOfDays());
        plan.setPlanName(libraryPlanDTO.getPlanName());
        if (libraryPlanDTO.getDescription() != null) plan.setDescription(libraryPlanDTO.getDescription());
        if (libraryPlanDTO.getIsActive() != null) plan.setIsActive(libraryPlanDTO.getIsActive());
        if (libraryPlanDTO.getGracePeriodDays() != null) plan.setGracePeriodDays(libraryPlanDTO.getGracePeriodDays());
        LibraryPlan saved = libraryPlanRepository.save(plan);
        return LibraryPlanMapper.toDto(saved);
    }

    @Override
    public ApiResponse deletePlan( Long id) throws Exception {
        LibraryPlan plan =  libraryPlanRepository.findById(id).orElseThrow(() -> new Exception("Plan not found"));

        long librariesOnPlan = libraryRepository.findAll().stream()
                .filter(lib -> lib.getLibraryPlan() != null && id.equals(lib.getLibraryPlan().getPlanId()))
                .count();
        boolean inUse = librariesOnPlan > 0;
        ApiResponse apiResponse = new ApiResponse();
        if (inUse) {
            // Never hard-delete a plan that libraries are currently subscribed to —
            // that would orphan their subscription/billing data. Soft-delete instead:
            // mark inactive so it disappears from "assign a new plan" lists but keeps
            // working for libraries already on it.
            plan.setIsActive(false);
            libraryPlanRepository.save(plan);
            apiResponse.setMessage("Plan is assigned to one or more libraries, so it was deactivated instead of deleted. It will no longer be offered to new libraries.");
        } else {
            libraryPlanRepository.delete(plan);
            apiResponse.setMessage("Plan Successfully deleted");
        }
        return apiResponse;
    }

    @Override
    public LibraryPlanDTO getPlan( Long id) throws Exception {
        LibraryPlan plan =  libraryPlanRepository.findById(id).orElseThrow(() -> new Exception("Plan not found"));
        return LibraryPlanMapper.toDto(plan);
    }

    @Override
    public List<LibraryPlanDTO> getAllPlans() throws Exception {
         List<LibraryPlan> plans = libraryPlanRepository.findAll();
         return plans.stream().map(LibraryPlanMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<LibraryPlanDTO> getActivePlans() throws Exception {
        List<LibraryPlan> plans = libraryPlanRepository.findByIsActiveTrue();
        return plans.stream().map(LibraryPlanMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public LibraryPlanDTO setPlanActive(Long id, boolean active) throws Exception {
        LibraryPlan plan = libraryPlanRepository.findById(id).orElseThrow(() -> new Exception("Plan not found"));
        plan.setIsActive(active);
        LibraryPlan saved = libraryPlanRepository.save(plan);
        return LibraryPlanMapper.toDto(saved);
    }
}
