package com.sentinelcore.service;

import com.sentinelcore.domain.entity.Benchmark;
import com.sentinelcore.domain.entity.BenchmarkRun;
import com.sentinelcore.domain.entity.EvaluationRun;
import com.sentinelcore.domain.enums.BenchmarkStatus;
import com.sentinelcore.domain.enums.RunMode;
import com.sentinelcore.domain.enums.StrategyType;
import com.sentinelcore.dto.BenchmarkExecutionResponse;
import com.sentinelcore.dto.BenchmarkReportResponse;
import com.sentinelcore.dto.DeltaMetrics;
import com.sentinelcore.dto.RunComparisonEntry;
import com.sentinelcore.dto.RunMetricsResponse;
import com.sentinelcore.repository.BenchmarkRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkService {

    private final BenchmarkRepository benchmarkRepository;
    private final EvaluationRunService runService;
    private final ReportingService reportingService;

    @Transactional
    public Benchmark createBenchmark(String model, List<StrategyType> strategyTypes) {
        LinkedHashSet<StrategyType> deduped = new LinkedHashSet<>();
        deduped.add(StrategyType.NONE);
        deduped.addAll(strategyTypes);

        Benchmark benchmark = new Benchmark();
        benchmark.setId("benchmark-" + UUID.randomUUID().toString().substring(0, 8));
        benchmark.setModel(model);
        benchmark.setStrategyTypes(new ArrayList<>(deduped));
        benchmark.setRuns(new ArrayList<>());
        benchmark.setStatus(BenchmarkStatus.CREATED);
        benchmark.setCreatedAt(LocalDateTime.now());
        return benchmarkRepository.save(benchmark);
    }

    public BenchmarkExecutionResponse executeBenchmark(String benchmarkId) {
        Benchmark benchmark = benchmarkRepository.findById(benchmarkId)
                .orElseThrow(() -> new EntityNotFoundException("Benchmark not found: " + benchmarkId));

        if (benchmark.getStatus() != BenchmarkStatus.CREATED) {
            throw new IllegalStateException(
                    "Benchmark " + benchmarkId + " cannot be executed in status: " + benchmark.getStatus()
            );
        }

        benchmark.setStatus(BenchmarkStatus.RUNNING);
        benchmark.setStartedAt(LocalDateTime.now());
        benchmarkRepository.saveAndFlush(benchmark);

        List<BenchmarkRun> completedRuns = new ArrayList<>();

        try {
            for (StrategyType strategyType : benchmark.getStrategyTypes()) {
                RunMode mode = (strategyType == StrategyType.NONE) ? RunMode.BASELINE : RunMode.DEFENDED;
                EvaluationRun run = runService.createRun(mode, benchmark.getModel(), strategyType);
                runService.executeRun(run.getId());
                completedRuns.add(new BenchmarkRun(strategyType, run.getId()));
                log.info("Benchmark {}: completed run {} with strategy {}",
                        benchmarkId, run.getId(), strategyType);
            }
            benchmark.setStatus(BenchmarkStatus.COMPLETED);
        } catch (RuntimeException ex) {
            benchmark.setStatus(BenchmarkStatus.FAILED);
            log.error("Benchmark {} failed after {} completed runs", benchmarkId, completedRuns.size(), ex);
            throw ex;
        } finally {
            benchmark.setRuns(completedRuns);
            benchmark.setFinishedAt(LocalDateTime.now());
            benchmarkRepository.save(benchmark);
        }

        return new BenchmarkExecutionResponse(
                benchmarkId,
                benchmark.getStatus().name(),
                benchmark.getStrategyTypes().size(),
                completedRuns.size()
        );
    }

    @Transactional(readOnly = true)
    public BenchmarkReportResponse getReport(String benchmarkId) {
        Benchmark benchmark = benchmarkRepository.findById(benchmarkId)
                .orElseThrow(() -> new EntityNotFoundException("Benchmark not found: " + benchmarkId));

        List<RunWithMetrics> runMetrics = benchmark.getRuns().stream()
                .map(br -> new RunWithMetrics(
                        br.getRunId(),
                        br.getStrategyType(),
                        reportingService.getMetrics(br.getRunId())
                ))
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
                benchmark.getStatus().name(),
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