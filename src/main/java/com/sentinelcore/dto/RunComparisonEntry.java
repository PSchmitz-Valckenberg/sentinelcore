package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.StrategyType;

import java.util.List;

public record RunComparisonEntry(
        List<String> runIds,
        StrategyType strategyType,
        RunMetricsResponse metrics,
        AggregatedStrategyMetrics aggregated,
        DeltaMetrics deltaToBaseline
) {}