package com.smp.conversation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateConversationRequestDto(
        @NotNull UUID doctorId,
        @NotNull UUID patientId) {
}
