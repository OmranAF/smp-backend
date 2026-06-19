package com.smp.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendConversationMessageRequestDto(
        @NotBlank @Size(max = 4000) String content) {
}
