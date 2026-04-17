package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.EvaluationCaseType;
import com.sentinelcore.domain.enums.ResultLabel;

import java.util.List;

public record ExecutionDto(
    String id,
    String caseId,
    String caseTitle,
    EvaluationCaseType caseType,
    String response,
    boolean blocked,
    boolean refused,
    ResultLabel label,
    long latencyMs,
    List<ScoreDetailDto> scoreDetails
) {}