package com.smp.appointment.dto;

import java.time.LocalDateTime;

public record FreeAppointmentSlotDto(LocalDateTime startTime, LocalDateTime endTime) {
}
