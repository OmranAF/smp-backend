package com.smp.appointment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.smp.appointment.AppointmentStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppointmentRequestDto(
        @NotNull UUID doctorId,
        @NotNull UUID patientId,
        @NotNull UUID serviceId,
        @NotNull LocalDateTime appointmentTime,
        AppointmentStatus status,
        @Size(max = 500) String reason,
        @Size(max = 1000) String notes) {
}