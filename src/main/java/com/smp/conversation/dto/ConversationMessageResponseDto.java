package com.smp.conversation.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.smp.conversation.ConversationSenderRole;

public record ConversationMessageResponseDto(
        UUID id,
        ConversationSenderRole senderRole,
        String senderName,
        String content,
        LocalDateTime createdAt,
        List<ConversationAttachmentResponseDto> attachments) {
}
