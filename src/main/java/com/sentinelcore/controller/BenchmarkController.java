package com.sentinelcore.controller;

import com.sentinelcore.domain.entity.Benchmark;
import com.sentinelcore.dto.BenchmarkCreateRequest;
import com.sentinelcore.dto.BenchmarkCreateResponse;
import com.sentinelcore.dto.BenchmarkExecutionResponse;
import com.sentinelcore.dto.BenchmarkReportResponse;
import com.sentinelcore.service.BenchmarkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/benchmarks")
@RequiredArgsConstructor
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    @PostMapping
    public ResponseEntity<BenchmarkCreateResponse> createBenchmark(@Valid @RequestBody BenchmarkCreateRequest request) {
        Benchmark benchmark = benchmarkService.createBenchmark(request.model(), request.strategyTypes());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new BenchmarkCreateResponse(
                        benchmark.getId(),
                        benchmark.getStatus(),
                        benchmark.getStrategyTypes()
                ));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<BenchmarkExecutionResponse> executeBenchmark(@PathVariable String id) {
        return ResponseEntity.ok(benchmarkService.executeBenchmark(id));
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<BenchmarkReportResponse> getReport(@PathVariable String id) {
        return ResponseEntity.ok(benchmarkService.getReport(id));
    }
}