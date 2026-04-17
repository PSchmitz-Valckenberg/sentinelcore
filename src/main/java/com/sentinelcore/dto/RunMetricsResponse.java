package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.EvaluationCaseType;

import java.util.Map;

public record RunMetricsResponse(
    String runId,
    String mode,
    String status,
    UtilityMetrics utilityMetrics,
    SecurityMetrics securityMetrics,
    Map<EvaluationCaseType, AttackCategoryMetrics> attackCategoryBreakdown
) {
    public record UtilityMetrics(
        long totalCases,
        long successCount,
        long partialSuccessCount,
        long failureCount,
        double successRate,
        double avgLatencyMs
    ) {}

    public record SecurityMetrics(
        long blockedCount,
        long refusedCount,
        double blockRate,
        double refusalRate
    ) {}

    public record AttackCategoryMetrics(
        long totalCases,
        long successCount,
        long partialSuccessCount,
        long failureCount,
        long blockedCount,
        long refusedCount,
        double successRate,
        double blockRate,
        double avgLatencyMs
    ) {}
}