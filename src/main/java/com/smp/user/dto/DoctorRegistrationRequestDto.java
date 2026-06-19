package com.smp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DoctorRegistrationRequestDto(
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String specialization,
        @Email @NotBlank String email,
        @NotBlank String password) {
}
