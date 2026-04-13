package com.sentinelcore.dto;

public record AskResponse(
    String answer,
    long latencyMs
) {}