package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.EvaluationCaseType;
import com.sentinelcore.domain.enums.ResultLabel;

public record ExecutionDto(
    String id,
    String caseId,
    EvaluationCaseType caseType,
    String response,
    boolean blocked,
    boolean refused,
    ResultLabel label,
    long latencyMs
) {}