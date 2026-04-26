package com.sentinelcore.defense;

public record RagDocumentVerdict(
    String content,
    boolean suspicious,
    String matchedPattern
) {
    public static RagDocumentVerdict clean(String content) {
        return new RagDocumentVerdict(content, false, null);
    }

    public static RagDocumentVerdict suspicious(String content, String matchedPattern) {
        return new RagDocumentVerdict(content, true, matchedPattern);
    }
}
