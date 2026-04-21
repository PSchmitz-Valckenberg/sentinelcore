package com.sentinelcore.controller;

import com.sentinelcore.domain.entity.Benchmark;
import com.sentinelcore.dto.BenchmarkCreateRequest;
import com.sentinelcore.dto.BenchmarkExecutionResponse;
import com.sentinelcore.dto.BenchmarkReportResponse;
import com.sentinelcore.service.BenchmarkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/benchmarks")
@RequiredArgsConstructor
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    @PostMapping
    public ResponseEntity<Benchmark> createBenchmark(@Valid @RequestBody BenchmarkCreateRequest request) {
        return ResponseEntity.ok(benchmarkService.createBenchmark(request.model(), request.strategyTypes()));
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