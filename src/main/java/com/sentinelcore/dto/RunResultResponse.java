package com.sentinelcore.dto;

import java.time.Instant;
import java.util.List;

public record RunResultResponse(
    String runId,
    String status,
    String mode,
    String model,
    Instant startedAt,
    Instant finishedAt,
    long totalCases,
    long completedCases,
    List<ExecutionDto> executions
) {}