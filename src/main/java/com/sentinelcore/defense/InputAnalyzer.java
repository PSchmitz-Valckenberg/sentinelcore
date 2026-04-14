package com.sentinelcore.defense;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InputAnalyzer {

    private final DefenseConfig defenseConfig;

    /**
     * Analyzes user input for injection patterns.
     * Returns a blocked DefenseResult if a keyword match is found, otherwise allowed.
     */
    public DefenseResult analyze(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return DefenseResult.allowed();
        }

        String normalized = userInput.toLowerCase(Locale.ROOT).strip();
        java.util.List<String> inputKeywords = defenseConfig.inputKeywords();
        if (inputKeywords == null || inputKeywords.isEmpty()) {
            return DefenseResult.allowed();
        }

        for (String keyword : inputKeywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                log.debug("Input blocked - matched keyword: '{}'", keyword);
                return DefenseResult.blocked("Matched injection keyword: " + keyword);
            }
        }

        return DefenseResult.allowed();
    }
}