package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.AttackCategory;

import java.util.Map;

public record RunMetricsResponse(
    String runId,
    String mode,
    String status,
    UtilityMetrics utilityMetrics,
    SecurityMetrics securityMetrics,
    Map<AttackCategory, AttackCategoryMetrics> attackCategoryBreakdown
) {
    public record UtilityMetrics(
        long totalCases,
        long successCount,
        long partialSuccessCount,
        long failureCount,
        double attackSuccessRate,
        double partialSuccessRate,
        double avgLatencyMs
    ) {}

    public record SecurityMetrics(
        long blockedCount,
        long refusedCount,
        double falsePositiveRate,
        double refusalRate
    ) {}

    public record AttackCategoryMetrics(
        long totalCases,
        long successCount,
        long partialSuccessCount,
        long failureCount,
        long blockedCount,
        long refusedCount,
        double attackSuccessRate,
        double partialSuccessRate,
        double blockRate,
        double avgLatencyMs
    ) {}
}