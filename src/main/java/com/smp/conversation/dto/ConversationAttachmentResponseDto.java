package com.smp.conversation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationAttachmentResponseDto(
        UUID id,
        String fileName,
        String contentType,
        long fileSize,
        LocalDateTime uploadedAt,
        boolean savedToPatientProfile) {
}
