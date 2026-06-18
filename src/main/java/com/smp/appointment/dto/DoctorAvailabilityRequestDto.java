package com.smp.appointment.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DoctorAvailabilityRequestDto(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Min(5) @Max(240) int slotDurationMinutes,
        Boolean active) {
}
