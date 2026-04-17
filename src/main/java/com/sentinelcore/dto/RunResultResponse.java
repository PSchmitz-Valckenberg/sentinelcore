package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.RunMode;
import com.sentinelcore.domain.enums.RunStatus;

import java.time.LocalDateTime;
import java.util.List;

public record RunResultResponse(
	String runId,
	RunStatus status,
	RunMode mode,
	String model,
	LocalDateTime startedAt,
	LocalDateTime finishedAt,
	long totalCases,
	long completedCases,
	List<ExecutionDto> executions
) {}