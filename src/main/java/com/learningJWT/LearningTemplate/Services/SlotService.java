package com.learningJWT.LearningTemplate.Services;

import com.learningJWT.LearningTemplate.Paylod.DTO.SlotDTO;
import java.util.List;

public interface SlotService {
    SlotDTO createSlot(SlotDTO dto) throws Exception;
    SlotDTO updateSlot(Long slotId, SlotDTO dto) throws Exception;
    void deleteSlot(Long slotId) throws Exception;
    List<SlotDTO> getAllSlots() throws Exception;
    List<SlotDTO> getSlotsByPlan(Long planId) throws Exception;
}
