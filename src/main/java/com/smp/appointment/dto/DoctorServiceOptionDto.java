package com.smp.appointment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DoctorServiceOptionDto(UUID id, String name, int durationMinutes, BigDecimal price) {
}