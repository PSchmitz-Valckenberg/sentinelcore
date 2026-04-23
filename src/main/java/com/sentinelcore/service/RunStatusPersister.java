package com.sentinelcore.service;

import com.sentinelcore.domain.enums.RunStatus;
import com.sentinelcore.repository.EvaluationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Persists run failure state in a dedicated REQUIRES_NEW transaction.
 *
 * This must be a separate Spring bean — not a method on EvaluationRunService itself.
 * Self-invocation within the same bean bypasses the Spring AOP proxy, which means
 * @Transactional(REQUIRES_NEW) would be silently ignored and the failure status
 * would be rolled back together with the outer transaction.
 *
 * Fields persisted here: status, finishedAt, startedAt, caseSuiteFingerprint.
 * startedAt and caseSuiteFingerprint are included because they are set within the
 * outer @Transactional on executeRun() and would otherwise be lost on rollback.
 */
@Component
@RequiredArgsConstructor
public class RunStatusPersister {

    private final EvaluationRunRepository runRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistFailure(String runId, Instant startedAt, String caseSuiteFingerprint) {
        var run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("Evaluation run not found for failure persistence: " + runId));

        run.setStatus(RunStatus.FAILED);
        run.setStartedAt(startedAt);
        run.setCaseSuiteFingerprint(caseSuiteFingerprint);
        run.setFinishedAt(Instant.now());
        runRepository.save(run);
    }
}