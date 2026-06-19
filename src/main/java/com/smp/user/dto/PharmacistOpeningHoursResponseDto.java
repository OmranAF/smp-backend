package com.smp.user.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record PharmacistOpeningHoursResponseDto(
        UUID id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active) {
}
