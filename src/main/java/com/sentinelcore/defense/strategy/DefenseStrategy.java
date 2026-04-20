package com.sentinelcore.defense.strategy;

import com.sentinelcore.domain.enums.StrategyType;

import java.util.List;
import java.util.function.Supplier;

public interface DefenseStrategy {

    StrategyType type();

    StrategyExecutionResult execute(String userInput, Supplier<List<String>> ragContentLoader);
}
