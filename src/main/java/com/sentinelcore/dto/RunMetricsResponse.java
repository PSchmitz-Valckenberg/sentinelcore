package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.EvaluationCaseType;
import com.sentinelcore.domain.enums.ResultLabel;

import java.util.Map;

public record RunMetricsResponse(
    String runId,
    long totalCases,
    long successCount,
    long partialSuccessCount,
    long failureCount,
    long blockedCount,
    long refusedCount,
    double successRate,
    double blockRate,
    double avgLatencyMs,
    Map<EvaluationCaseType, Long> countByCaseType,
    Map<ResultLabel, Long> countByLabel
) {}