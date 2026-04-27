package com.sentinelcore.dto;

import java.util.List;

public record BenchmarkReportResponse(
        String benchmarkId,
        String model,
        String status,
        int repetitions,
        List<RunComparisonEntry> runs
) {}