package com.smp.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientHealthStatusRequestDto(
        @NotBlank String status,
        @Size(max = 1000) String notes) {
}