package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.StrategyType;

public record RunComparisonEntry(
        String runId,
        StrategyType strategyType,
        RunMetricsResponse metrics,
        DeltaMetrics deltaToBaseline
) {}