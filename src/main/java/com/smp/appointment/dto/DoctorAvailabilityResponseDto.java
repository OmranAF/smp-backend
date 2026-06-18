package com.smp.appointment.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record DoctorAvailabilityResponseDto(
        UUID id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes,
        boolean active) {
}
