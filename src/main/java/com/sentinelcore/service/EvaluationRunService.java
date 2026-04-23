package com.sentinelcore.service;

import com.sentinelcore.defense.strategy.DefenseStrategy;
import com.sentinelcore.defense.strategy.DefenseStrategyRegistry;
import com.sentinelcore.defense.strategy.StrategyExecutionResult;
import com.sentinelcore.defense.strategy.SystemPromptBuilder;
import com.sentinelcore.domain.config.SystemPromptConfig;
import com.sentinelcore.domain.entity.AttackExecution;
import com.sentinelcore.domain.entity.EvaluationCase;
import com.sentinelcore.domain.entity.EvaluationRun;
import com.sentinelcore.domain.entity.ScoreDetail;
import com.sentinelcore.domain.enums.ResultLabel;
import com.sentinelcore.domain.enums.RunMode;
import com.sentinelcore.domain.enums.RunStatus;
import com.sentinelcore.domain.enums.StrategyType;
import com.sentinelcore.repository.AttackExecutionRepository;
import com.sentinelcore.repository.EvaluationCaseRepository;
import com.sentinelcore.repository.EvaluationRunRepository;
import com.sentinelcore.repository.ScoreDetailRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class EvaluationRunService {

    private final EvaluationRunRepository runRepository;
    private final AttackExecutionRepository executionRepository;
    private final ScoreDetailRepository scoreDetailRepository;
    private final EvaluationCaseRepository caseRepository;
    private final ScoringEngine scoringEngine;
    private final DefenseStrategyRegistry strategyRegistry;
    private final SystemPromptConfig systemPromptConfig;
    private final SystemPromptBuilder systemPromptBuilder;
    private final CaseSuiteHasher caseSuiteHasher;

    public EvaluationRun createRun(RunMode mode, String model, StrategyType strategyType) {
        StrategyType resolved = strategyType != null
                ? strategyType
                : (mode == RunMode.DEFENDED ? StrategyType.INPUT_OUTPUT : StrategyType.NONE);

        // Snapshot the fully-built system prompt — including the appended canary token —
        // so the run record reflects exactly what was sent to the LLM.
        String builtPrompt = systemPromptBuilder.build();

        EvaluationRun run = EvaluationRun.builder()
                .id("run-" + UUID.randomUUID().toString().substring(0, 8))
                .mode(mode)
                .status(RunStatus.CREATED)
                .model(model)
                .strategyType(resolved)
                .systemPromptSnapshot(builtPrompt)
                .canaryTokenSnapshot(systemPromptConfig.canaryToken())
                .createdAt(Instant.now())
                .build();

        return runRepository.save(run);
    }

    @Transactional
    public EvaluationRun executeRun(String runId) {
        EvaluationRun run = runRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));

        if (run.getStatus() != RunStatus.CREATED) {
            throw new IllegalStateException("Run already started or completed");
        }

        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        runRepository.save(run);

        List<EvaluationCase> cases = caseRepository.findAll();
        String fingerprint = caseSuiteHasher.compute(cases);
        run.setCaseSuiteFingerprint(fingerprint);

        log.info("Executing run {} with {} cases in {} mode (suite fingerprint: {})",
                runId, cases.size(), run.getMode(), fingerprint);

        try {
            DefenseStrategy strategy = strategyRegistry.get(run.getStrategyType());
            for (EvaluationCase evalCase : cases) {
                processCase(run, evalCase, strategy);
            }
            run.setStatus(RunStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Run {} failed during execution", runId, e);
            // Persist FAILED status in a separate transaction so it is not rolled back
            // together with the outer @Transactional when we rethrow.
            persistFailureStatus(runId);
            run.setStatus(RunStatus.FAILED);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Run " + runId + " failed during execution", e);
        }
        run.setFinishedAt(Instant.now());
        return runRepository.save(run);
    }

    /**
     * Persists FAILED status + finishedAt in a dedicated REQUIRES_NEW transaction.
     * This ensures failure state survives even when the outer executeRun transaction
     * is rolled back on exception.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistFailureStatus(String runId) {
        runRepository.findById(runId).ifPresent(run -> {
            run.setStatus(RunStatus.FAILED);
            run.setFinishedAt(Instant.now());
            runRepository.save(run);
        });
    }

    private void processCase(EvaluationRun run, EvaluationCase evalCase, DefenseStrategy strategy) {
        Supplier<List<String>> ragContentLoader = () -> resolveRagDocuments(evalCase);
        StrategyExecutionResult result = strategy.execute(evalCase.getUserInput(), ragContentLoader);

        String responseText = result.answer() != null ? result.answer() : "";
        boolean blocked = result.blocked();
        boolean refused = result.refused();
        long latencyMs = result.latencyMs();

        List<ScoringEngine.CheckResult> allChecks =
                scoringEngine.runAllChecks(evalCase.getUserInput(), responseText);
        ResultLabel label = scoringEngine.determineFinalLabel(allChecks, evalCase.getRelevantChecks());

        AttackExecution execution = AttackExecution.builder()
                .id("exec-" + UUID.randomUUID().toString().substring(0, 8))
                .run(run)
                .evaluationCase(evalCase)
                .caseType(evalCase.getCaseType())
                .response(responseText)
                .blocked(blocked)
                .refused(refused)
                .label(label)
                .latencyMs(Math.toIntExact(latencyMs))
                .build();
        executionRepository.save(execution);

        for (ScoringEngine.CheckResult check : allChecks) {
            ScoreDetail detail = ScoreDetail.builder()
                    .id("score-" + UUID.randomUUID().toString().substring(0, 8))
                    .execution(execution)
                    .checkType(check.type())
                    .result(isSuccessfulResult(check.result()))
                    .evidence(buildPersistedEvidence(check))
                    .build();
            scoreDetailRepository.save(detail);
        }
    }

    private boolean isSuccessfulResult(ResultLabel resultLabel) {
        return resultLabel == ResultLabel.SUCCESS || resultLabel == ResultLabel.PARTIAL_SUCCESS;
    }

    private String buildPersistedEvidence(ScoringEngine.CheckResult check) {
        String resultLabel = check.result() != null ? check.result().name() : "UNKNOWN";
        String evidence = check.evidence();
        if (evidence == null || evidence.isBlank()) {
            return "resultLabel=" + resultLabel;
        }
        return "resultLabel=" + resultLabel + "; evidence=" + evidence;
    }

    private List<String> resolveRagDocuments(EvaluationCase evalCase) {
        return evalCase.getRagDocuments().stream()
                .map(doc -> doc.getTitle() + "\n" + doc.getContent())
                .toList();
    }
}