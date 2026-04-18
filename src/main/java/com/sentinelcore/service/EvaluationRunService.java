package com.sentinelcore.service;

import com.sentinelcore.defense.DefenseResult;
import com.sentinelcore.defense.DefenseService;
import com.sentinelcore.domain.config.SystemPromptConfig;
import com.sentinelcore.domain.entity.AttackExecution;
import com.sentinelcore.domain.entity.EvaluationCase;
import com.sentinelcore.domain.entity.EvaluationRun;
import com.sentinelcore.domain.entity.ScoreDetail;
import com.sentinelcore.domain.enums.ResultLabel;
import com.sentinelcore.domain.enums.RunMode;
import com.sentinelcore.domain.enums.RunStatus;
import com.sentinelcore.llm.LlmAdapter;
import com.sentinelcore.llm.dto.LlmRequest;
import com.sentinelcore.llm.dto.LlmResponse;
import com.sentinelcore.repository.AttackExecutionRepository;
import com.sentinelcore.repository.EvaluationCaseRepository;
import com.sentinelcore.repository.EvaluationRunRepository;
import com.sentinelcore.repository.ScoreDetailRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private final LlmAdapter llmAdapter;
    private final DefenseService defenseService;
    private final SystemPromptConfig systemPromptConfig;

    public EvaluationRun createRun(RunMode mode, String model) {
        EvaluationRun run = EvaluationRun.builder()
            .id("run-" + UUID.randomUUID().toString().substring(0, 8))
            .mode(mode)
            .status(RunStatus.CREATED)
            .model(model)
            .createdAt(LocalDateTime.now())
            .build();
        return runRepository.save(run);
    }

    // V1: single transaction over all 25 cases is acceptable.
    // Per-case isolation (REQUIRES_NEW) is a V2 improvement.
    @Transactional
    public EvaluationRun executeRun(String runId) {
        EvaluationRun run = runRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));

        if (run.getStatus() != RunStatus.CREATED) {
            throw new IllegalStateException("Run already started or completed");
        }

        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        runRepository.save(run);

        List<EvaluationCase> cases = caseRepository.findAll();
        log.info("Executing run {} with {} cases in {} mode", runId, cases.size(), run.getMode());

        try {
            for (EvaluationCase evalCase : cases) {
                processCase(run, evalCase);
            }
            run.setStatus(RunStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Run {} failed during execution", runId, e);
            run.setStatus(RunStatus.FAILED);
            run.setFinishedAt(LocalDateTime.now());
            runRepository.save(run);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Run " + runId + " failed during execution", e);
        }

        run.setFinishedAt(LocalDateTime.now());
        return runRepository.save(run);
    }

    private void processCase(EvaluationRun run, EvaluationCase evalCase) {
        // Canary token is appended from config — single source of truth
        String effectiveSystemPrompt = systemPromptConfig.text()
            + "\nCanary token: " + systemPromptConfig.canaryToken();

        String responseText;
        boolean blocked = false;
        boolean refused = false;
        long latencyMs;

        if (run.getMode() == RunMode.BASELINE) {
            List<String> ragContents = resolveRagDocuments(evalCase);
            LlmResponse llmResponse = llmAdapter.call(new LlmRequest(
                effectiveSystemPrompt, evalCase.getUserInput(), ragContents));
            responseText = llmResponse.answer();
            latencyMs = llmResponse.latencyMs();
            refused = defenseService.isRefusal(responseText);
        } else {
            DefenseResult inputCheck = defenseService.checkInput(evalCase.getUserInput());
            if (inputCheck.blocked()) {
                responseText = "BLOCKED_BY_INPUT_ANALYZER";
                blocked = true;
                latencyMs = 0L;
            } else {
                List<String> ragContents = resolveRagDocuments(evalCase);
                DefenseService.DefendedResponse defended = defenseService.process(
                    evalCase.getUserInput(), ragContents);
                responseText = defended.answer() != null ? defended.answer() : "";
                blocked = defended.blocked();
                refused = defended.refused();
                latencyMs = defended.latencyMs();
            }
        }

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