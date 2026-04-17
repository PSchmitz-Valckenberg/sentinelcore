package com.sentinelcore.service;

import com.sentinelcore.domain.entity.AttackExecution;
import com.sentinelcore.domain.entity.EvaluationRun;
import com.sentinelcore.domain.entity.ScoreDetail;
import com.sentinelcore.domain.enums.EvaluationCaseType;
import com.sentinelcore.domain.enums.ResultLabel;
import com.sentinelcore.dto.ExecutionDto;
import com.sentinelcore.dto.RunMetricsResponse;
import com.sentinelcore.dto.RunMetricsResponse.AttackCategoryMetrics;
import com.sentinelcore.dto.RunMetricsResponse.SecurityMetrics;
import com.sentinelcore.dto.RunMetricsResponse.UtilityMetrics;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        Set<String> executionIds = executions.stream()
            .map(AttackExecution::getId)
            .collect(Collectors.toSet());

        Map<String, List<ScoreDetail>> scoreDetailsByExecutionId = scoreDetailRepository.findAll().stream()
            .filter(d -> d.getExecution() != null && executionIds.contains(d.getExecution().getId()))
            .collect(Collectors.groupingBy(d -> d.getExecution().getId()));

        List<ExecutionDto> executionDtos = executions.stream()
            .map(e -> toExecutionDto(e, scoreDetailsByExecutionId))
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

        // --- Utility metrics (across all cases) ---
        long successCount = count(executions, ResultLabel.SUCCESS);
        long partialSuccessCount = count(executions, ResultLabel.PARTIAL_SUCCESS);
        long failureCount = count(executions, ResultLabel.FAIL);
        double avgLatencyMs = round1(executions.stream()
            .mapToLong(AttackExecution::getLatencyMs).average().orElse(0.0));

        // successRate = successful or partially successful attack cases / total attack cases
        List<AttackExecution> attackExecs = filterByCaseType(executions, EvaluationCaseType.ATTACK);
        long successfulAttackCount = attackExecs.stream()
            .filter(e -> e.getLabel() == ResultLabel.SUCCESS || e.getLabel() == ResultLabel.PARTIAL_SUCCESS)
            .count();
        double successRate = round3(attackExecs.isEmpty() ? 0.0
            : (double) successfulAttackCount / attackExecs.size());

        UtilityMetrics utilityMetrics = new UtilityMetrics(
            totalCases, successCount, partialSuccessCount, failureCount, successRate, avgLatencyMs);

        // --- Security metrics ---
        long blockedCount = executions.stream().filter(AttackExecution::isBlocked).count();
        long refusedCount = executions.stream().filter(AttackExecution::isRefused).count();

        // blockRate = blocked benign cases / total benign cases (false positive rate)
        List<AttackExecution> benignExecs = filterByCaseType(executions, EvaluationCaseType.BENIGN);
        long blockedBenignCount = benignExecs.stream().filter(AttackExecution::isBlocked).count();
        double blockRate = round3(benignExecs.isEmpty() ? 0.0
            : (double) blockedBenignCount / benignExecs.size());
        double refusalRate = round3(executions.isEmpty() ? 0.0
            : (double) refusedCount / executions.size());

        SecurityMetrics securityMetrics = new SecurityMetrics(
            blockedCount, refusedCount, blockRate, refusalRate);

        // --- Breakdown by case type ---
        Map<EvaluationCaseType, AttackCategoryMetrics> breakdown = executions.stream()
            .collect(Collectors.groupingBy(AttackExecution::getCaseType))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> buildCategoryMetrics(e.getValue())
            ));

        log.info("Metrics for run {}: successRate={}, blockRate={}, refusalRate={}, avgLatency={}ms",
            runId, successRate, blockRate, refusalRate, avgLatencyMs);

        return new RunMetricsResponse(
            run.getId(),
            run.getMode().name(),
            run.getStatus().name(),
            utilityMetrics,
            securityMetrics,
            breakdown
        );
    }

    // --- Helpers ---

    private AttackCategoryMetrics buildCategoryMetrics(List<AttackExecution> execs) {
        long success = count(execs, ResultLabel.SUCCESS);
        long partial = count(execs, ResultLabel.PARTIAL_SUCCESS);
        long failure = count(execs, ResultLabel.FAIL);
        long blocked = execs.stream().filter(AttackExecution::isBlocked).count();
        long refused = execs.stream().filter(AttackExecution::isRefused).count();
        long successfulAttacks = success + partial;
        double sr = round3(execs.isEmpty() ? 0.0 : (double) successfulAttacks / execs.size());
        double br = round3(execs.isEmpty() ? 0.0 : (double) blocked / execs.size());
        double avg = round1(execs.stream().mapToLong(AttackExecution::getLatencyMs).average().orElse(0.0));
        return new AttackCategoryMetrics(execs.size(), success, partial, failure, blocked, refused, sr, br, avg);
    }

    private long count(List<AttackExecution> execs, ResultLabel label) {
        return execs.stream().filter(e -> e.getLabel() == label).count();
    }

    private List<AttackExecution> filterByCaseType(List<AttackExecution> execs, EvaluationCaseType type) {
        return execs.stream().filter(e -> e.getCaseType() == type).toList();
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private ExecutionDto toExecutionDto(AttackExecution execution,
                                        Map<String, List<ScoreDetail>> scoreDetailsByExecutionId) {
        List<ScoreDetail> details = scoreDetailsByExecutionId
            .getOrDefault(execution.getId(), Collections.emptyList());
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