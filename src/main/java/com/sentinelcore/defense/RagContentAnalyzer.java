package com.sentinelcore.defense;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Component
public class RagContentAnalyzer {

    private final List<Pattern> compiledPatterns;

    public RagContentAnalyzer(DefenseConfig defenseConfig) {
        List<Pattern> patterns = new ArrayList<>();
        for (String raw : defenseConfig.ragContentPatterns()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                patterns.add(Pattern.compile(raw, Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
            } catch (PatternSyntaxException e) {
                throw new IllegalStateException(
                        "Invalid regex in sentinelcore.defense.rag-content-patterns: \"%s\" — %s"
                                .formatted(raw, e.getMessage()), e);
            }
        }
        this.compiledPatterns = List.copyOf(patterns);
    }

    public RagDocumentVerdict analyze(String content) {
        if (content == null || content.isBlank() || compiledPatterns.isEmpty()) {
            return RagDocumentVerdict.clean(content);
        }
        for (Pattern pattern : compiledPatterns) {
            if (pattern.matcher(content).find()) {
                log.debug("RAG document flagged as suspicious - matched pattern: '{}'", pattern.pattern());
                return RagDocumentVerdict.suspicious(content, pattern.pattern());
            }
        }
        return RagDocumentVerdict.clean(content);
    }
}
