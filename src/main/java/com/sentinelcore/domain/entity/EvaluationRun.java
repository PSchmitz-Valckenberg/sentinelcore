package com.sentinelcore.domain.entity;

import com.sentinelcore.domain.enums.RunMode;
import com.sentinelcore.domain.enums.RunStatus;
import com.sentinelcore.domain.enums.StrategyType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationRun {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private RunMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RunStatus status;

    @Column(name = "model", nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false)
    private StrategyType strategyType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
