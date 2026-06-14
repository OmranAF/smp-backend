package com.smp.appointment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.smp.appointment.AppointmentStatus;

public record AppointmentResponseDto(
        UUID id,
        UUID doctorId,
        String doctorName,
        UUID patientId,
        String patientName,
        UUID serviceId,
        String serviceName,
        LocalDateTime appointmentTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String reason,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}