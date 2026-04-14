package com.sentinelcore.defense;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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

        for (String phrase : defenseConfig.outputPhrases()) {
            if (output.contains(phrase)) {
                log.debug("Output blocked - matched sensitive phrase: '{}'", phrase);
                return DefenseResult.blocked("Output contained sensitive phrase: " + phrase);
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

        for (String phrase : REFUSAL_PHRASES) {
            if (output.contains(phrase)) {
                log.debug("Refusal detected - matched phrase: '{}'", phrase);
                return true;
            }
        }

        return false;
    }
}