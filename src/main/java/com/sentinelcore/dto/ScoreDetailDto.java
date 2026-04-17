package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.CheckType;

public record ScoreDetailDto(
    String id,
    CheckType checkType,
    boolean result,
    String evidence
) {}