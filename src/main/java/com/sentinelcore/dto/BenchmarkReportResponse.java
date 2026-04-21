package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.BenchmarkStatus;

import java.util.List;

public record BenchmarkReportResponse(
        String benchmarkId,
        String model,
        BenchmarkStatus status,
        List<RunComparisonEntry> runs
) {}