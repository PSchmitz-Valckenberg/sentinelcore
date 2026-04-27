package com.sentinelcore.service;

import com.sentinelcore.domain.entity.Benchmark;
import com.sentinelcore.domain.entity.BenchmarkRun;
import com.sentinelcore.domain.entity.EvaluationRun;
import com.sentinelcore.domain.enums.BenchmarkStatus;
import com.sentinelcore.domain.enums.RunMode;
import com.sentinelcore.domain.enums.StrategyType;
import com.sentinelcore.dto.AggregatedStrategyMetrics;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkService {

    private final BenchmarkRepository benchmarkRepository;
    private final EvaluationRunService runService;
    private final ReportingService reportingService;

    @Transactional
    public Benchmark createBenchmark(String model, List<StrategyType> strategyTypes, int repetitions) {
        LinkedHashSet<StrategyType> deduped = new LinkedHashSet<>();
        deduped.add(StrategyType.NONE);
        deduped.addAll(strategyTypes);

        Benchmark benchmark = new Benchmark();
        benchmark.setId("benchmark-" + UUID.randomUUID().toString().substring(0, 8));
        benchmark.setModel(model);
        benchmark.setStrategyTypes(new ArrayList<>(deduped));
        benchmark.setRuns(new ArrayList<>());
        benchmark.setRepetitions(repetitions);
        benchmark.setStatus(BenchmarkStatus.CREATED);
        benchmark.setCreatedAt(Instant.now());
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
        benchmark.setStartedAt(Instant.now());
        benchmarkRepository.saveAndFlush(benchmark);

        List<BenchmarkRun> completedRuns = new ArrayList<>();
        int repetitions = benchmark.getRepetitions();

        try {
            for (StrategyType strategyType : benchmark.getStrategyTypes()) {
                RunMode mode = (strategyType == StrategyType.NONE) ? RunMode.BASELINE : RunMode.DEFENDED;
                for (int rep = 0; rep < repetitions; rep++) {
                    EvaluationRun run = runService.createRun(mode, benchmark.getModel(), strategyType);
                    runService.executeRun(run.getId());
                    completedRuns.add(new BenchmarkRun(strategyType, run.getId(), rep));
                    log.info("Benchmark {}: completed run {} (strategy={}, rep={}/{})",
                            benchmarkId, run.getId(), strategyType, rep + 1, repetitions);
                }
            }
            benchmark.setStatus(BenchmarkStatus.COMPLETED);
        } catch (RuntimeException ex) {
            benchmark.setStatus(BenchmarkStatus.FAILED);
            log.error("Benchmark {} failed after {} completed runs", benchmarkId, completedRuns.size(), ex);
            throw ex;
        } finally {
            benchmark.setRuns(completedRuns);
            benchmark.setFinishedAt(Instant.now());
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

        // Group runs by strategy, preserving insertion order
        Map<StrategyType, List<RunMetricsResponse>> metricsByStrategy = new LinkedHashMap<>();
        Map<StrategyType, List<String>> runIdsByStrategy = new LinkedHashMap<>();
        for (BenchmarkRun br : benchmark.getRuns()) {
            metricsByStrategy.computeIfAbsent(br.getStrategyType(), k -> new ArrayList<>())
                    .add(reportingService.getMetrics(br.getRunId()));
            runIdsByStrategy.computeIfAbsent(br.getStrategyType(), k -> new ArrayList<>())
                    .add(br.getRunId());
        }

        // Use the first NONE run as baseline for delta computation
        List<RunMetricsResponse> baselineRuns = metricsByStrategy.get(StrategyType.NONE);
        RunMetricsResponse baselineSample = (baselineRuns != null && !baselineRuns.isEmpty())
                ? baselineRuns.get(0) : null;

        List<RunComparisonEntry> entries = new ArrayList<>();
        for (Map.Entry<StrategyType, List<RunMetricsResponse>> e : metricsByStrategy.entrySet()) {
            StrategyType strategy = e.getKey();
            List<RunMetricsResponse> runs = e.getValue();
            List<String> runIds = runIdsByStrategy.get(strategy);

            // Representative single-run metrics for backwards-compatible fields
            RunMetricsResponse representative = runs.get(0);
            AggregatedStrategyMetrics aggregated = aggregate(runs);
            DeltaMetrics delta = (baselineSample != null && strategy != StrategyType.NONE)
                    ? computeDelta(baselineSample, representative) : null;

            entries.add(new RunComparisonEntry(runIds, strategy, representative, aggregated, delta));
        }

        return new BenchmarkReportResponse(
                benchmarkId,
                benchmark.getModel(),
                benchmark.getStatus().name(),
                benchmark.getRepetitions(),
                entries
        );
    }

    // --- Statistics ---

    static AggregatedStrategyMetrics aggregate(List<RunMetricsResponse> runs) {
        int n = runs.size();
        double[] asr = extract(runs, r -> r.metrics().attackSuccessRate());
        double[] fpr = extract(runs, r -> r.metrics().falsePositiveRate());
        double[] rr  = extract(runs, r -> r.metrics().refusalRate());
        double[] lat = extract(runs, r -> r.metrics().avgLatencyMs());
        return new AggregatedStrategyMetrics(
                n,
                mean(asr), stddev(asr),
                mean(fpr), stddev(fpr),
                mean(rr),  stddev(rr),
                mean(lat), stddev(lat)
        );
    }

    private static double[] extract(List<RunMetricsResponse> runs,
                                    java.util.function.ToDoubleFunction<RunMetricsResponse> fn) {
        double[] vals = new double[runs.size()];
        for (int i = 0; i < runs.size(); i++) {
            vals[i] = fn.applyAsDouble(runs.get(i));
        }
        return vals;
    }

    static double mean(double[] values) {
        if (values.length == 0) return 0.0;
        double sum = 0;
        for (double v : values) sum += v;
        return round3(sum / values.length);
    }

    // Returns null for N=1 (not computable), otherwise population stddev
    static Double stddev(double[] values) {
        if (values.length <= 1) return null;
        double m = mean(values);
        double sumSq = 0;
        for (double v : values) sumSq += (v - m) * (v - m);
        return round3(Math.sqrt(sumSq / values.length));
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

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
