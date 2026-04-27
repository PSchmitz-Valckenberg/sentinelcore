package com.sentinelcore.domain.entity;

import com.sentinelcore.domain.enums.StrategyType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BenchmarkRun {

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false)
    private StrategyType strategyType;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "repetition_index", nullable = false)
    private int repetitionIndex;
}