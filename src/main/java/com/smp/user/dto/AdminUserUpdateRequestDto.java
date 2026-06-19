package com.smp.user.dto;

public record AdminUserUpdateRequestDto(
        Boolean enabled,
        Boolean doctorApproved) {
}
