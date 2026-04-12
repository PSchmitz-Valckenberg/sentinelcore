package com.sentinelcore.loader.dto;

import com.sentinelcore.domain.enums.DocumentTrustLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RagDocumentDto(

    @NotBlank
    String id,

    @NotBlank
    String title,

    @NotBlank
    String content,

    @NotNull
    DocumentTrustLevel trustLevel
) {}
