package com.smp.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DoctorAdminRegistrationDto(
        UUID doctorId,
        String name,
        String email,
        String specialization,
        boolean active,
        LocalDateTime registeredAt) {
}
