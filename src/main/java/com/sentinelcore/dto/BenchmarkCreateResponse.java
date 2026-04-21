package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.StrategyType;

import java.util.List;

public record BenchmarkCreateResponse(
        String benchmarkId,
        String status,
        List<StrategyType> strategyTypes
) {}