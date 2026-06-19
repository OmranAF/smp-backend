package com.smp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PharmacistRegistrationRequestDto(
        @NotBlank String name,
        @NotBlank String address,
        @Email @NotBlank String email,
        @NotBlank String password) {
}
