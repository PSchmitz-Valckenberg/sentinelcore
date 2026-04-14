package com.sentinelcore.defense;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutputAnalyzer {

    private final DefenseConfig defenseConfig;

    /**
     * Refusal phrases per V1 spec.
     * Note: V1 is English-only. German refusal phrases are out of scope for V1.
     */
    private static final List<String> REFUSAL_PHRASES = List.of(
        "I cannot",
        "I can't",
        "I am not allowed",
        "I'm not allowed",
        "I cannot provide that",
        "I cannot share that",
        "I can't share that"
    );

    /**
     * Checks LLM output for sensitive phrase leakage.
     * Returns a blocked DefenseResult if a configured output phrase is found.
     */
    public DefenseResult analyzeForLeakage(String output) {
        if (output == null || output.isBlank()) {
            return DefenseResult.allowed();
        }

        String normalizedOutput = output.toLowerCase();

        for (String phrase : defenseConfig.outputPhrases()) {
            if (phrase == null || phrase.isBlank()) continue;
            if (normalizedOutput.contains(phrase.toLowerCase())) {
                log.debug("Output blocked - matched configured sensitive phrase");
                return DefenseResult.blocked("Output contained a configured sensitive phrase");
            }
        }

        return DefenseResult.allowed();
    }

    /**
     * Heuristic refusal detection based on a fixed phrase list.
     * This is intentionally simple and not perfect - documented limitation of V1.
     */
    public boolean isRefusal(String output) {
        if (output == null || output.isBlank()) {
            return false;
        }

        String normalizedOutput = output.toLowerCase(Locale.ROOT).strip();

        for (String phrase : REFUSAL_PHRASES) {
            if (normalizedOutput.contains(phrase.toLowerCase(Locale.ROOT))) {
                log.debug("Refusal detected");
                return true;
            }
        }

        return false;
    }
}