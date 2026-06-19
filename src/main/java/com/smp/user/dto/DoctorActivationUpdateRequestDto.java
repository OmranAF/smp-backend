package com.smp.user.dto;

import jakarta.validation.constraints.NotNull;

public record DoctorActivationUpdateRequestDto(@NotNull Boolean active) {
}
