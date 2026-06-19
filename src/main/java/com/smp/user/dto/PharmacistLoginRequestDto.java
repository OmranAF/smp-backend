package com.smp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PharmacistLoginRequestDto(
        @Email @NotBlank String email,
        @NotBlank String password) {
}
