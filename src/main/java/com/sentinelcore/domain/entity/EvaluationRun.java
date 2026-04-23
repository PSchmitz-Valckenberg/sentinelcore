package com.sentinelcore.domain.entity;

import com.sentinelcore.domain.enums.RunMode;
import com.sentinelcore.domain.enums.RunStatus;
import com.sentinelcore.domain.enums.StrategyType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

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

    // Reproducibility snapshot — captured at createRun() time
    @Column(name = "system_prompt_snapshot", columnDefinition = "TEXT")
    private String systemPromptSnapshot;

    @Column(name = "canary_token_snapshot", length = 200)
    private String canaryTokenSnapshot;

    // Captured at executeRun() time — SHA-256 over all case IDs + userInputs
    @Column(name = "case_suite_fingerprint", length = 64)
    private String caseSuiteFingerprint;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
