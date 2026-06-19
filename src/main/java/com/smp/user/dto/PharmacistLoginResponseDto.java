package com.smp.user.dto;

import java.util.UUID;

public record PharmacistLoginResponseDto(
        UUID pharmacistId,
        String name,
        String address,
        String email) {
}
