package com.smp.prescription.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DoctorPrescriptionResponseDto(
        UUID id,
        UUID doctorId,
        String doctorName,
        UUID patientId,
        String patientName,
        UUID appointmentId,
        UUID ticketId,
        String qrCodeImageUrl,
        String medicationName,
        String dosage,
        String instructions,
        String fulfillmentStatus,
        LocalDateTime issuedAt,
        LocalDate validUntil) {
}
