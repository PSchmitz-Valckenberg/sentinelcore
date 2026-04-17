package com.sentinelcore.service;

import com.sentinelcore.domain.entity.AttackExecution;
import com.sentinelcore.domain.entity.EvaluationCase;
import com.sentinelcore.domain.entity.EvaluationRun;
import com.sentinelcore.domain.entity.ScoreDetail;
import com.sentinelcore.domain.enums.AttackCategory;
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
import jakarta.persistence.EntityManager;
import com.sentinelcore.repository.ScoreDetailRepository;
import jakarta.persistence.PersistenceContext;
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
    @PersistenceContext
    private EntityManager entityManager;


    @Transactional(readOnly = true)
    public RunResultResponse getResults(String runId) {
        EvaluationRun run = runRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));

        List<AttackExecution> executions = executionRepository.findByRunId(runId);
        long totalCases = caseRepository.count();

        Set<String> executionIds = executions.stream()
            .map(AttackExecution::getId)
            .collect(Collectors.toSet());
        Map<String, List<ScoreDetail>> scoreDetailsByExecutionId = executionIds.isEmpty()
            ? Collections.emptyMap()
            : entityManager.createQuery(
                "select sd from ScoreDetail sd join fetch sd.execution e where e.id in :executionIds",
                ScoreDetail.class
            )
            .setParameter("executionIds", executionIds)
            .getResultList()
            .stream()
            ? Collections.emptyMap()
            : scoreDetailRepository.findByExecutionIdIn(executionIds).stream()
                .collect(Collectors.groupingBy(d -> d.getExecution().getId()));

        List<ExecutionDto> executionDtos = executions.stream()
            .map(e -> toExecutionDto(e, scoreDetailsByExecutionId))
            .toList();

        return new RunResultResponse(
            run.getId(), run.getStatus(), run.getMode(), run.getModel(),
            run.getStartedAt(), run.getFinishedAt(), totalCases, executions.size(), executionDtos);
    }

    @Transactional(readOnly = true)
    public RunMetricsResponse getMetrics(String runId) {
        EvaluationRun run = runRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));

        List<AttackExecution> executions = executionRepository.findByRunId(runId);
        long totalCases = caseRepository.count();

        // Utility metrics (across all cases)
        long successCount = count(executions, ResultLabel.SUCCESS);
        long partialSuccessCount = count(executions, ResultLabel.PARTIAL_SUCCESS);
        long failureCount = count(executions, ResultLabel.FAIL);
        double avgLatencyMs = round1(executions.stream()
            .mapToLong(AttackExecution::getLatencyMs).average().orElse(0.0));

        // attackSuccessRate and partialSuccessRate computed over ATTACK cases only
        List<AttackExecution> attackExecs = filterByCaseType(executions, EvaluationCaseType.ATTACK);
        double attackSuccessRate = round3(attackExecs.isEmpty() ? 0.0
            : (double) count(attackExecs, ResultLabel.SUCCESS) / attackExecs.size());
        double partialSuccessRate = round3(attackExecs.isEmpty() ? 0.0
            : (double) count(attackExecs, ResultLabel.PARTIAL_SUCCESS) / attackExecs.size());

        UtilityMetrics utilityMetrics = new UtilityMetrics(
            totalCases, successCount, partialSuccessCount, failureCount,
            attackSuccessRate, partialSuccessRate, avgLatencyMs);

        // Security metrics
        long blockedCount = executions.stream().filter(AttackExecution::isBlocked).count();
        long refusedCount = executions.stream().filter(AttackExecution::isRefused).count();

        // falsePositiveRate = blocked or refused BENIGN cases / total BENIGN cases
        List<AttackExecution> benignExecs = filterByCaseType(executions, EvaluationCaseType.BENIGN);
        long falsePositiveBenignCount = benignExecs.stream()
            .filter(e -> e.isBlocked() || e.isRefused()).count();
        double falsePositiveRate = round3(benignExecs.isEmpty() ? 0.0
            : (double) falsePositiveBenignCount / benignExecs.size());
        double refusalRate = round3(executions.isEmpty() ? 0.0
            : (double) refusedCount / executions.size());

        SecurityMetrics securityMetrics = new SecurityMetrics(
            blockedCount, refusedCount, falsePositiveRate, refusalRate);

        // Breakdown by AttackCategory (ATTACK cases only)
        Map<AttackCategory, AttackCategoryMetrics> breakdown = executions.stream()
            .filter(e -> e.getCaseType() == EvaluationCaseType.ATTACK)
            .filter(e -> e.getEvaluationCase() != null)
            .filter(e -> e.getEvaluationCase().getAttackCategory() != null)
            .collect(Collectors.groupingBy(e -> e.getEvaluationCase().getAttackCategory()))
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> buildCategoryMetrics(e.getValue())));

        log.info("Metrics for run {}: attackSuccessRate={}, partialSuccessRate={}, falsePositiveRate={}, refusalRate={}, avgLatency={}ms",
            runId, attackSuccessRate, partialSuccessRate, falsePositiveRate, refusalRate, avgLatencyMs);

        return new RunMetricsResponse(
            run.getId(), run.getMode().name(), run.getStatus().name(),
            utilityMetrics, securityMetrics, breakdown);
    }

    // --- Helpers ---

    private AttackCategoryMetrics buildCategoryMetrics(List<AttackExecution> execs) {
        long success = count(execs, ResultLabel.SUCCESS);
        long partial = count(execs, ResultLabel.PARTIAL_SUCCESS);
        long failure = count(execs, ResultLabel.FAIL);
        long blocked = execs.stream().filter(AttackExecution::isBlocked).count();
        long refused = execs.stream().filter(AttackExecution::isRefused).count();
        double asr = round3(execs.isEmpty() ? 0.0 : (double) success / execs.size());
        double psr = round3(execs.isEmpty() ? 0.0 : (double) partial / execs.size());
        double br = round3(execs.isEmpty() ? 0.0 : (double) blocked / execs.size());
        double avg = round1(execs.stream().mapToLong(AttackExecution::getLatencyMs).average().orElse(0.0));
        return new AttackCategoryMetrics(
            execs.size(), success, partial, failure, blocked, refused, asr, psr, br, avg);
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

    private List<ExecutionDto> toExecutionDtos(List<AttackExecution> executions,
                                               Map<String, List<ScoreDetail>> scoreDetailsByExecutionId) {
        Set<String> evaluationCaseIds = executions.stream()
            .map(AttackExecution::getEvaluationCase)
            .filter(java.util.Objects::nonNull)
            .map(EvaluationCase::getId)
            .collect(Collectors.toSet());

        Map<String, EvaluationCase> evaluationCasesById = evaluationCaseRepository.findAllById(evaluationCaseIds).stream()
            .collect(Collectors.toMap(EvaluationCase::getId, evaluationCase -> evaluationCase));

        return executions.stream()
            .map(execution -> toExecutionDto(execution, scoreDetailsByExecutionId, evaluationCasesById))
            .toList();
    }

    private ExecutionDto toExecutionDto(AttackExecution execution,
                                        Map<String, List<ScoreDetail>> scoreDetailsByExecutionId) {
        return toExecutionDto(execution, scoreDetailsByExecutionId, Collections.emptyMap());
    }

    private ExecutionDto toExecutionDto(AttackExecution execution,
                                        Map<String, List<ScoreDetail>> scoreDetailsByExecutionId,
                                        Map<String, EvaluationCase> evaluationCasesById) {
        List<ScoreDetail> details = scoreDetailsByExecutionId
            .getOrDefault(execution.getId(), Collections.emptyList());
        List<ScoreDetailDto> detailDtos = details.stream()
            .map(d -> new ScoreDetailDto(d.getId(), d.getCheckType(), d.isResult(), d.getEvidence()))
            .toList();

        String evaluationCaseId = execution.getEvaluationCase().getId();
        EvaluationCase evaluationCase = evaluationCasesById.get(evaluationCaseId);
        if (evaluationCase == null) {
            evaluationCase = evaluationCaseRepository.findById(evaluationCaseId)
                .orElseThrow(() -> new EntityNotFoundException("Evaluation case not found: " + evaluationCaseId));
        }

        return new ExecutionDto(
            execution.getId(),
            evaluationCase.getId(),
            evaluationCase.getName(),
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