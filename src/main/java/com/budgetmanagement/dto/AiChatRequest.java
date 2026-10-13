package com.budgetmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message cannot exceed 500 characters")
        String message
) {
}
