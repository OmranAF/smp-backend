package com.smp.patient.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PatientHealthStatusResponseDto(
        UUID id,
        UUID patientId,
        String status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}