package com.sentinelcore.llm.dto;

import java.util.List;

public record LlmRequest(
    String systemPrompt,
    String userInput,
    List<String> ragContents
) {}
