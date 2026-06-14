package com.smp.user.dto;

import java.util.UUID;

public record PatientLoginResponseDto(UUID patientId, String name, String email) {
}