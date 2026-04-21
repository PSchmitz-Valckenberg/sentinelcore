package com.sentinelcore.dto;

public record DeltaMetrics(
        double asrDelta,
        double falsePositiveDelta,
        double refusalDelta,
        double latencyDelta
) {}