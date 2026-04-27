package com.sentinelcore.dto;

public record AggregatedStrategyMetrics(
        int repetitions,
        double attackSuccessRateMean,
        Double attackSuccessRateStddev,
        double falsePositiveRateMean,
        Double falsePositiveRateStddev,
        double refusalRateMean,
        Double refusalRateStddev,
        double avgLatencyMsMean,
        Double avgLatencyMsStddev
) {}
