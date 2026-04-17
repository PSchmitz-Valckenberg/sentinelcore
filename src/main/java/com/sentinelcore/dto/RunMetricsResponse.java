package com.sentinelcore.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sentinelcore.domain.enums.AttackCategory;

import java.util.Map;

public record RunMetricsResponse(
    String runId,
    String mode,
    String status,
    @JsonIgnore UtilityMetrics utilityMetrics,
    @JsonIgnore SecurityMetrics securityMetrics,
    @JsonIgnore Map<AttackCategory, AttackCategoryMetrics> attackCategoryBreakdown
) {
    @JsonProperty("metrics")
    public Metrics metrics() {
        return new Metrics(
            utilityMetrics.totalCases(),
            utilityMetrics.successCount(),
            utilityMetrics.partialSuccessCount(),
            utilityMetrics.failureCount(),
            securityMetrics.blockedCount(),
            securityMetrics.refusedCount(),
            utilityMetrics.attackSuccessRate(),
            utilityMetrics.partialSuccessRate(),
            securityMetrics.falsePositiveRate(),
            securityMetrics.refusalRate(),
            utilityMetrics.avgLatencyMs()
        );
    }

    @JsonProperty("breakdown")
    public Map<AttackCategory, AttackCategoryMetrics> breakdown() {
        return attackCategoryBreakdown;
    }

    public record Metrics(
        long totalCases,
        long successCount,
        long partialSuccessCount,
        long failureCount,
        long blockedCount,
        long refusedCount,
        double attackSuccessRate,
        double partialSuccessRate,
        double falsePositiveRate,
        double refusalRate,
        double avgLatencyMs
    ) {}

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