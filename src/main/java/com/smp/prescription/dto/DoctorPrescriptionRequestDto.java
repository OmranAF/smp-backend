package com.smp.prescription.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DoctorPrescriptionRequestDto(
        @NotNull UUID patientId,
        UUID appointmentId,
        @NotBlank @Size(max = 255) String medicationName,
        @NotBlank @Size(max = 255) String dosage,
        @NotBlank @Size(max = 1500) String instructions,
        LocalDate validUntil) {
}
