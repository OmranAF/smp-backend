package com.smp.conversation.dto;

import java.util.UUID;

public record ConversationPartnerDto(
        UUID id,
        String name,
        String email,
        String subtitle) {
}
