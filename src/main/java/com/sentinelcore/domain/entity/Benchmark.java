package com.sentinelcore.domain.entity;

import com.sentinelcore.domain.enums.BenchmarkStatus;
import com.sentinelcore.domain.enums.StrategyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "benchmarks")
@Getter
@Setter
public class Benchmark {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "model", nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BenchmarkStatus status;

    @ElementCollection
    @CollectionTable(
            name = "benchmark_strategies",
            joinColumns = @JoinColumn(name = "benchmark_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false)
    @OrderColumn(name = "strategy_order")
    private List<StrategyType> strategyTypes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "benchmark_runs",
            joinColumns = @JoinColumn(name = "benchmark_id")
    )
        private List<BenchmarkRun> runs = new ArrayList<>();

        @Column(name = "repetitions", nullable = false)
        private int repetitions = 1;

        @Column(name = "created_at", nullable = false)
        private Instant createdAt;

        @Column(name = "started_at")
        private Instant startedAt;

        @Column(name = "finished_at")
        private Instant finishedAt;
}