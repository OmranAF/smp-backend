package com.smp.user.dto;

import java.util.UUID;

public record PharmacistRegistrationResponseDto(
        UUID pharmacistId,
        String name,
        String address,
        String email) {
}
