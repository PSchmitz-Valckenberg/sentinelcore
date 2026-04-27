package com.sentinelcore.dto;

import com.sentinelcore.domain.enums.StrategyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BenchmarkCreateRequest(
        @NotBlank String model,
        @NotEmpty List<@NotNull StrategyType> strategyTypes,
        @Min(1) @Max(10) Integer repetitions
) {
    public int repetitionsOrDefault() {
        return repetitions != null ? repetitions : 1;
    }
}