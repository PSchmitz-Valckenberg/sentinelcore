package com.sentinelcore.service;

import com.sentinelcore.domain.entity.AttackExecution;
import com.sentinelcore.domain.entity.EvaluationRun;
import com.sentinelcore.domain.entity.ScoreDetail;
import com.sentinelcore.domain.enums.EvaluationCaseType;
import com.sentinelcore.domain.enums.ResultLabel;
import com.sentinelcore.dto.ExecutionDto;
import com.sentinelcore.dto.RunMetricsResponse;
import com.sentinelcore.dto.RunResultResponse;
import com.sentinelcore.dto.ScoreDetailDto;
import com.sentinelcore.repository.AttackExecutionRepository;
import com.sentinelcore.repository.EvaluationCaseRepository;
import com.sentinelcore.repository.EvaluationRunRepository;
import com.sentinelcore.repository.ScoreDetailRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ReportingService {

    private final EvaluationRunRepository runRepository;
    private final AttackExecutionRepository executionRepository;
    private final ScoreDetailRepository scoreDetailRepository;
    private final EvaluationCaseRepository caseRepository;

    @Transactional(readOnly = true)
    public RunResultResponse getResults(String runId) {
        EvaluationRun run = runRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));

        List<AttackExecution> executions = executionRepository.findByRunId(runId);
        long totalCases = caseRepository.count();

        List<ExecutionDto> executionDtos = executions.stream()
            .map(this::toExecutionDto)
            .toList();

        return new RunResultResponse(
            run.getId(),
            run.getStatus(),
            run.getMode(),
            run.getModel(),
            run.getStartedAt(),
            run.getFinishedAt(),
            totalCases,
            executions.size(),
            executionDtos
        );
    }

    @Transactional(readOnly = true)
    public RunMetricsResponse getMetrics(String runId) {
        EvaluationRun run = runRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));

        List<AttackExecution> executions = executionRepository.findByRunId(runId);
        long totalCases = caseRepository.count();

        long successCount = executions.stream()
            .filter(e -> e.getLabel() == ResultLabel.SUCCESS).count();
        long partialSuccessCount = executions.stream()
            .filter(e -> e.getLabel() == ResultLabel.PARTIAL_SUCCESS).count();
        long failureCount = executions.stream()
            .filter(e -> e.getLabel() == ResultLabel.FAIL).count();
        long blockedCount = executions.stream()
            .filter(AttackExecution::isBlocked).count();
        long refusedCount = executions.stream()
            .filter(AttackExecution::isRefused).count();

        long attackExecutionCount = executions.stream()
            .filter(e -> e.getCaseType() == EvaluationCaseType.ATTACK)
            .count();
        long benignExecutionCount = executions.stream()
            .filter(e -> e.getCaseType() == EvaluationCaseType.BENIGN)
            .count();
        long successfulAttackCount = executions.stream()
            .filter(e -> e.getCaseType() == EvaluationCaseType.ATTACK)
            .filter(e -> e.getLabel() == ResultLabel.SUCCESS || e.getLabel() == ResultLabel.PARTIAL_SUCCESS)
            .count();
        long blockedBenignCount = executions.stream()
            .filter(e -> e.getCaseType() == EvaluationCaseType.BENIGN)
            .filter(AttackExecution::isBlocked)
            .count();

        double successRate = attackExecutionCount == 0 ? 0.0
            : (double) successfulAttackCount / attackExecutionCount;
        double blockRate = benignExecutionCount == 0 ? 0.0
            : (double) blockedBenignCount / benignExecutionCount;
        double refusalRate = executions.isEmpty() ? 0.0
            : (double) refusedCount / executions.size();
        double avgLatencyMs = executions.stream()
            .mapToLong(AttackExecution::getLatencyMs)
            .average()
            .orElse(0.0);

        Map<EvaluationCaseType, Long> countByCaseType = executions.stream()
            .collect(Collectors.groupingBy(AttackExecution::getCaseType, Collectors.counting()));
        Map<ResultLabel, Long> countByLabel = executions.stream()
            .collect(Collectors.groupingBy(AttackExecution::getLabel, Collectors.counting()));

        log.info("Metrics for run {}: successRate={}, blockRate={}, refusalRate={}, avgLatency={}ms",
            runId, String.format("%.3f", successRate),
            String.format("%.3f", blockRate),
            String.format("%.3f", refusalRate),
            String.format("%.1f", avgLatencyMs));

        return new RunMetricsResponse(
            run.getId(),
            totalCases,
            successCount,
            partialSuccessCount,
            failureCount,
            blockedCount,
            refusedCount,
            Math.round(successRate * 1000.0) / 1000.0,
            Math.round(blockRate * 1000.0) / 1000.0,
            Math.round(avgLatencyMs * 10.0) / 10.0,
            countByCaseType,
            countByLabel
        );
    }

    private ExecutionDto toExecutionDto(AttackExecution execution) {
        List<ScoreDetail> details = scoreDetailRepository.findByExecutionId(execution.getId());
        List<ScoreDetailDto> detailDtos = details.stream()
            .map(d -> new ScoreDetailDto(d.getId(), d.getCheckType(), d.isResult(), d.getEvidence()))
            .toList();

        return new ExecutionDto(
            execution.getId(),
            execution.getEvaluationCase().getId(),
            execution.getEvaluationCase().getName(),
            execution.getCaseType(),
            execution.getResponse(),
            execution.isBlocked(),
            execution.isRefused(),
            execution.getLabel(),
            execution.getLatencyMs(),
            detailDtos
        );
    }
}