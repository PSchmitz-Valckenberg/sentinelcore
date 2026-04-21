package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.BenchmarkStatus;

public record BenchmarkExecutionResponse(
        String benchmarkId,
        BenchmarkStatus status,
        int totalRuns,
        int completedRuns
) {}