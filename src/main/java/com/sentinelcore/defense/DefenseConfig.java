package com.sentinelcore.defense;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "sentinelcore.defense")
public record DefenseConfig(
    List<String> inputKeywords,
    List<String> outputPhrases
) {
    public DefenseConfig {
        inputKeywords = inputKeywords == null ? List.of() : inputKeywords;
        outputPhrases = outputPhrases == null ? List.of() : outputPhrases;
    }
}