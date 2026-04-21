package com.sentinelcore.dto;

public record BenchmarkExecutionResponse(
        String benchmarkId,
        String status,
        int totalRuns,
        int completedRuns
) {}