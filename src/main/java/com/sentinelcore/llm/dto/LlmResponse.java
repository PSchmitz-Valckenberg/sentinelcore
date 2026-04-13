package com.sentinelcore.llm.dto;

public record LlmResponse(
    String answer,
    long latencyMs
) {}
