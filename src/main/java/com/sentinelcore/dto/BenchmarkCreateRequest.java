package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.StrategyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BenchmarkCreateRequest(
        @NotBlank String model,
        @NotEmpty List<@NotNull StrategyType> strategyTypes
) {}