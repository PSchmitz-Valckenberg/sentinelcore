package com.sentinelcore.defense.strategy;

public record StrategyExecutionResult(
    String answer,
    boolean blocked,
    boolean refused,
    long latencyMs
) {}
