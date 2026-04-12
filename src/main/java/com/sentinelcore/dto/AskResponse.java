package com.sentinelcore.dto;

public record AskResponse(
    String systemPrompt,
    String userMessage,
    String llmResponse
) {}
