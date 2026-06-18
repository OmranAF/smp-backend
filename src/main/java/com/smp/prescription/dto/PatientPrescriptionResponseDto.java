package com.smp.prescription.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PatientPrescriptionResponseDto(
        UUID id,
        UUID doctorId,
        String doctorName,
        UUID patientId,
        String patientName,
        UUID appointmentId,
        UUID ticketId,
        String ticketToken,
        String qrCodeImageUrl,
        String ticketDetailsUrl,
        String medicationName,
        String dosage,
        String instructions,
        LocalDateTime issuedAt,
        LocalDate validUntil) {
}
