package com.sentinelcore.dto;

public record RunExecutionResponse(
    String runId,
    String status,
    String mode,
    long totalCases,
    long completedCases
) {}