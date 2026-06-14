package com.smp.patient.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PatientDocumentResponseDto(
        UUID id,
        String fileName,
        String contentType,
        long fileSize,
        String description,
        LocalDateTime uploadedAt) {
}