package com.smp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DoctorLoginRequestDto(
        @Email @NotBlank String email,
        @NotBlank String password) {
}
