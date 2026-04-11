package com.sentinelcore.domain.entity;

import com.sentinelcore.domain.enums.EvaluationCaseType;
import com.sentinelcore.domain.enums.ResultLabel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attack_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttackExecution {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private EvaluationRun run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private EvaluationCase evaluationCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false)
    private EvaluationCaseType caseType;

    @Column(name = "response", nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(name = "blocked", nullable = false)
    private boolean blocked;

    @Column(name = "refused", nullable = false)
    private boolean refused;

    @Enumerated(EnumType.STRING)
    @Column(name = "label", nullable = false)
    private ResultLabel label;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;
}
