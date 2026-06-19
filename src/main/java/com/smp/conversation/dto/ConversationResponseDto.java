package com.smp.conversation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponseDto(
        UUID id,
        UUID doctorId,
        String doctorName,
        UUID patientId,
        String patientName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
