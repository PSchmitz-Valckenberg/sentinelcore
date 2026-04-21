package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.BenchmarkStatus;
import com.sentinelcore.domain.enums.StrategyType;

import java.util.List;

public record BenchmarkCreateResponse(
        String benchmarkId,
        BenchmarkStatus status,
        List<StrategyType> strategyTypes
) {}