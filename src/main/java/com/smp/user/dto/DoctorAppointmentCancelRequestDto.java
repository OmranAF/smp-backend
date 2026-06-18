package com.smp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoctorAppointmentCancelRequestDto(
        @NotBlank @Size(max = 500) String cancellationReason) {
}
