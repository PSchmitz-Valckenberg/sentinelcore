package com.sentinelcore.service;

import com.sentinelcore.domain.entity.Benchmark;
import com.sentinelcore.domain.entity.EvaluationRun;
import com.sentinelcore.domain.enums.BenchmarkStatus;
import com.sentinelcore.domain.enums.RunMode;
import com.sentinelcore.domain.enums.StrategyType;
import com.sentinelcore.dto.*;
import com.sentinelcore.repository.BenchmarkRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkService {

    private final BenchmarkRepository benchmarkRepository;
    private final EvaluationRunService runService;
    private final ReportingService reportingService;

    public Benchmark createBenchmark(String model, List<StrategyType> strategyTypes) {
        Benchmark benchmark = new Benchmark();
        benchmark.setId("benchmark-" + UUID.randomUUID().toString().substring(0, 8));
        benchmark.setModel(model);
        benchmark.setStrategyTypes(new ArrayList<>(strategyTypes));
        benchmark.setRunIds(new ArrayList<>());
        benchmark.setStatus(BenchmarkStatus.CREATED);
        benchmark.setCreatedAt(Instant.now());
        return benchmarkRepository.save(benchmark);
    }

    @Transactional
    public BenchmarkExecutionResponse executeBenchmark(String benchmarkId) {
        Benchmark benchmark = benchmarkRepository.findById(benchmarkId)
                .orElseThrow(() -> new EntityNotFoundException("Benchmark not found: " + benchmarkId));

        benchmark.setStatus(BenchmarkStatus.RUNNING);
        benchmark.setStartedAt(Instant.now());
        benchmarkRepository.save(benchmark);

        List<String> runIds = new ArrayList<>();

        for (StrategyType strategyType : benchmark.getStrategyTypes()) {
            RunMode mode = (strategyType == StrategyType.NONE) ? RunMode.BASELINE : RunMode.DEFENDED;
            EvaluationRun run = runService.createRun(mode, benchmark.getModel(), strategyType);
            runService.executeRun(run.getId());
            runIds.add(run.getId());
            log.info("Benchmark {}: completed run {} with strategy {}",
                    benchmarkId, run.getId(), strategyType);
        }

        benchmark.setRunIds(runIds);
        benchmark.setStatus(BenchmarkStatus.COMPLETED);
        benchmark.setFinishedAt(Instant.now());
        benchmarkRepository.save(benchmark);

        return new BenchmarkExecutionResponse(
                benchmarkId,
                BenchmarkStatus.COMPLETED,
                runIds.size(),
                runIds.size()
        );
    }

    @Transactional(readOnly = true)
    public BenchmarkReportResponse getReport(String benchmarkId) {
        Benchmark benchmark = benchmarkRepository.findById(benchmarkId)
                .orElseThrow(() -> new EntityNotFoundException("Benchmark not found: " + benchmarkId));

        List<RunWithMetrics> runMetrics = benchmark.getRunIds().stream()
                .map(runId -> {
                    RunMetricsResponse metrics = reportingService.getMetrics(runId);
                    StrategyType strategyType = runService.getStrategyType(runId);
                    return new RunWithMetrics(runId, strategyType, metrics);
                })
                .toList();

        RunMetricsResponse baseline = runMetrics.stream()
                .filter(r -> r.strategyType() == StrategyType.NONE)
                .map(RunWithMetrics::metrics)
                .findFirst()
                .orElse(null);

        List<RunComparisonEntry> entries = runMetrics.stream()
                .map(r -> new RunComparisonEntry(
                        r.runId(),
                        r.strategyType(),
                        r.metrics(),
                        (baseline != null && r.strategyType() != StrategyType.NONE)
                                ? computeDelta(baseline, r.metrics())
                                : null
                ))
                .toList();

        return new BenchmarkReportResponse(
                benchmarkId,
                benchmark.getModel(),
                benchmark.getStatus(),
                entries
        );
    }

    private DeltaMetrics computeDelta(RunMetricsResponse baseline, RunMetricsResponse defended) {
        RunMetricsResponse.Metrics b = baseline.metrics();
        RunMetricsResponse.Metrics d = defended.metrics();
        return new DeltaMetrics(
                round3(d.attackSuccessRate() - b.attackSuccessRate()),
                round3(d.falsePositiveRate() - b.falsePositiveRate()),
                round3(d.refusalRate() - b.refusalRate()),
                round3(d.avgLatencyMs() - b.avgLatencyMs())
        );
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record RunWithMetrics(String runId, StrategyType strategyType, RunMetricsResponse metrics) {}
}