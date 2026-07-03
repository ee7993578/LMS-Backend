package com.learningJWT.LearningTemplate.Mapper;

import com.learningJWT.LearningTemplate.Model.Attendance;
import com.learningJWT.LearningTemplate.Model.AttendanceSlot;
import com.learningJWT.LearningTemplate.Paylod.DTO.AttendanceDTO;
import com.learningJWT.LearningTemplate.Paylod.DTO.AttendanceSlotDTO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AttendanceMapper {

    public static AttendanceDTO toDTO(Attendance attendance) {
        if (attendance == null) return null;

        List<AttendanceSlotDTO> slotDTOs = attendance.getSlots() == null
                ? Collections.emptyList()
                : attendance.getSlots().stream()
                    .map(AttendanceMapper::slotToDTO)
                    .collect(Collectors.toList());

        return AttendanceDTO.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent() != null ? attendance.getStudent().getId() : null)
                .studentName(attendance.getStudent().getFullName() != null ? attendance.getStudent().getFullName() : null)
                .libraryId(attendance.getLibrary() != null ? attendance.getLibrary().getId() : null)
                .libraryName(attendance.getLibrary() != null ? attendance.getLibrary().getName() : null)
                .attendanceDate(attendance.getAttendanceDate())
                .shiftStart(attendance.getShiftStart())
                .shiftEnd(attendance.getShiftEnd())
                .attendanceStatus(attendance.getStatus())
                .totalStudyMinutes(attendance.getTotalStudyMinutes())
                .totalBreakMinutes(attendance.getTotalBreakMinutes())
                .slots(slotDTOs)
                .build();
    }

    private static AttendanceSlotDTO slotToDTO(AttendanceSlot slot) {
        return AttendanceSlotDTO.builder()
                .slotNumber(slot.getSlotNumber())
                .punchIn(slot.getPunchIn())
                .punchOut(slot.getPunchOut())
                .durationMinutes(slot.getDurationMinutes())
                .autoClosed(slot.getAutoClosed())
                .build();
    }

    public static Attendance toEntity(AttendanceDTO dto) {
        if (dto == null) return null;

        Attendance attendance = new Attendance();
        attendance.setId(dto.getId());
        attendance.setAttendanceDate(dto.getAttendanceDate());
        attendance.setShiftStart(dto.getShiftStart());
        attendance.setShiftEnd(dto.getShiftEnd());
        attendance.setStatus(dto.getAttendanceStatus());
        attendance.setTotalStudyMinutes(dto.getTotalStudyMinutes());
        attendance.setTotalBreakMinutes(dto.getTotalBreakMinutes());
        return attendance;
    }
}
