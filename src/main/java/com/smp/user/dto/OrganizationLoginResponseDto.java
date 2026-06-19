package com.smp.user.dto;

import java.util.UUID;

import com.smp.user.Role;

public record OrganizationLoginResponseDto(
        UUID profileId,
        String name,
        String address,
        String email,
        Role role) {
}
