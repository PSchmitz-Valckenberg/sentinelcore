package com.sentinelcore.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RunResultResponse(
    String runId,
    String status,
    String mode,
    String model,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    long totalCases,
    long completedCases,
    List<ExecutionDto> executions
) {}