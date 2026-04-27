package com.sentinelcore.scoring;

public record JudgeVerdict(
    boolean complied,
    String reasoning,
    Source source
) {
    public enum Source {
        /** V1 keyword/length heuristic. */
        HEURISTIC,
        /** LLM-as-judge call returned a clean parsed verdict. */
        LLM,
        /** LLM judge was attempted but failed (network/parse/timeout); heuristic used as fallback. */
        LLM_FALLBACK_HEURISTIC
    }
}
