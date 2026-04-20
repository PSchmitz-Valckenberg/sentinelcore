package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.RunMode;
import com.sentinelcore.domain.enums.StrategyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RunCreateRequest(
        @NotNull RunMode mode,
        @NotBlank String model,
        StrategyType strategyType
) {
}