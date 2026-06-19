package com.smp.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.smp.user.Role;

public record AdminUserSummaryDto(
        UUID userId,
        UUID profileId,
        String name,
        String email,
        Role role,
        boolean enabled,
        Boolean doctorApproved,
        String specialization,
        String address,
        LocalDateTime createdAt) {
}
