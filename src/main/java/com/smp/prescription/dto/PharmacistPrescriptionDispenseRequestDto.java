package com.smp.prescription.dto;

import java.util.UUID;

import com.smp.prescription.PrescriptionFulfillmentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PharmacistPrescriptionDispenseRequestDto(
        @NotNull UUID ticketId,
        @NotBlank String token,
        @NotNull PrescriptionFulfillmentStatus status,
        String note) {
}
