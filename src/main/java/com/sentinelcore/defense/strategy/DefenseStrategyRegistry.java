package com.sentinelcore.defense.strategy;

import com.sentinelcore.domain.enums.StrategyType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DefenseStrategyRegistry {

    private final Map<StrategyType, DefenseStrategy> strategies;

    public DefenseStrategyRegistry(List<DefenseStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        DefenseStrategy::type,
                        strategy -> strategy,
                        (existing, duplicate) -> {
                            throw new IllegalStateException(
                                    "Duplicate DefenseStrategy registration for type [%s]: %s vs %s".formatted(
                                            existing.type(),
                                            existing.getClass().getSimpleName(),
                                            duplicate.getClass().getSimpleName()
                                    )
                            );
                        }
                ));
    }

    public DefenseStrategy get(StrategyType type) {
        DefenseStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No DefenseStrategy registered for type: " + type);
        }
        return strategy;
    }
}
