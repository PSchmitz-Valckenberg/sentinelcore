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
    private String id;

    @Column(nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BenchmarkStatus status;

    @ElementCollection
    @CollectionTable(
            name = "benchmark_strategies",
            joinColumns = @JoinColumn(name = "benchmark_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type")
    private List<StrategyType> strategyTypes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "benchmark_runs",
            joinColumns = @JoinColumn(name = "benchmark_id")
    )
    @Column(name = "run_id")
    private List<String> runIds = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    private Instant startedAt;
    private Instant finishedAt;
}