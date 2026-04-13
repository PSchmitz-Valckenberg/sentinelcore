package com.sentinelcore.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AskRequest(
    @NotBlank String userInput,
    List<String> ragDocumentIds
) {}