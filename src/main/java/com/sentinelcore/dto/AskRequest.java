package com.sentinelcore.dto;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(
    @NotBlank String systemPrompt,
    @NotBlank String userMessage
) {}
