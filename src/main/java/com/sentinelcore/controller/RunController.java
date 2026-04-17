package com.sentinelcore.controller;

import com.sentinelcore.domain.entity.EvaluationRun;
import com.sentinelcore.dto.RunCreateRequest;
import com.sentinelcore.dto.RunExecutionResponse;
import com.sentinelcore.dto.RunMetricsResponse;
import com.sentinelcore.dto.RunResultResponse;
import com.sentinelcore.repository.AttackExecutionRepository;
import com.sentinelcore.repository.EvaluationCaseRepository;
import com.sentinelcore.service.EvaluationRunService;
import com.sentinelcore.service.ReportingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/runs")
@RequiredArgsConstructor
@Slf4j
public class RunController {

    private final EvaluationRunService runService;
    private final AttackExecutionRepository executionRepository;
    private final EvaluationCaseRepository caseRepository;
    private final ReportingService reportingService;

    @PostMapping
    public ResponseEntity<Map<String, String>> createRun(@Valid @RequestBody RunCreateRequest request) {
        EvaluationRun run = runService.createRun(request.mode(), request.model());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("runId", run.getId(), "status", run.getStatus().name()));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<RunExecutionResponse> executeRun(@PathVariable String id) {
        EvaluationRun run = runService.executeRun(id);
        long totalCases = caseRepository.count();
        long completedCases = executionRepository.countByRunId(id);
        return ResponseEntity.ok(new RunExecutionResponse(
            run.getId(),
            run.getStatus().name(),
            run.getMode().name(),
            totalCases,
            completedCases));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<RunResultResponse> getResults(@PathVariable String id) {
        return ResponseEntity.ok(reportingService.getResults(id));
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<RunMetricsResponse> getMetrics(@PathVariable String id) {
        return ResponseEntity.ok(reportingService.getMetrics(id));
    }
}