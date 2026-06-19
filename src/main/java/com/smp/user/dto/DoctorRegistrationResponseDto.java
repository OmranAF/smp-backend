package com.smp.user.dto;

import java.util.UUID;

public record DoctorRegistrationResponseDto(
        UUID doctorId,
        String name,
        String email,
        String specialization,
        boolean active,
        String statusMessage) {
}
