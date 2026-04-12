package com.sentinelcore.loader.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sentinelcore.domain.enums.AttackCategory;
import com.sentinelcore.domain.enums.CheckType;
import com.sentinelcore.domain.enums.EvaluationCaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EvaluationCaseDto(

    @NotBlank
    String id,

    @NotNull
    EvaluationCaseType caseType,

    AttackCategory attackCategory,

    @NotBlank
    String name,

    @NotBlank
    String userInput,

    @NotBlank
    String expectedBehavior,

    @JsonProperty("ragDocumentIds")
    List<String> ragDocumentIds,

    List<CheckType> relevantChecks
) {}
