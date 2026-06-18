package com.smp.user.dto;

import java.util.UUID;

public record DoctorLoginResponseDto(
        UUID doctorId,
        String name,
        String email,
        String specialization) {
}
