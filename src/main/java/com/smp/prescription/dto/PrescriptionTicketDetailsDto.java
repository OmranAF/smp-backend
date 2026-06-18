package com.smp.prescription.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PrescriptionTicketDetailsDto(
        UUID ticketId,
        String medicationName,
        String dosage,
        String instructions,
        LocalDateTime issuedAt,
        LocalDate validUntil,
        String doctorName,
        String patientName) {
}
