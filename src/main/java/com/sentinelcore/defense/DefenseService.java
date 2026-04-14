package com.sentinelcore.defense;

import com.sentinelcore.domain.config.SystemPromptConfig;
import com.sentinelcore.llm.LlmAdapter;
import com.sentinelcore.llm.dto.LlmRequest;
import com.sentinelcore.llm.dto.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefenseService {

    private final InputAnalyzer inputAnalyzer;
    private final OutputAnalyzer outputAnalyzer;
    private final LlmAdapter llmAdapter;
    private final SystemPromptConfig systemPromptConfig;

    private static final String BLOCKED_RESPONSE = "Your request could not be processed.";

    /**
     * Defended flow:
     * 1. Input Analyzer - block before LLM call if injection pattern detected
     * 2. LLM call - only if input passes
     * 3. Output Analyzer - check for sensitive phrase leakage
     * 4. Refusal Detection - heuristic check on LLM response
     */
    public DefendedResponse process(String userInput, List<String> ragContents) {
        long start = System.nanoTime();

        // Step 1: Input Analysis - block before LLM
        DefenseResult inputResult = inputAnalyzer.analyze(userInput);
        if (inputResult.blocked()) {
            long latency = (System.nanoTime() - start) / 1_000_000L;
            log.info("Request blocked by InputAnalyzer - reason: {}", inputResult.reason());
            return new DefendedResponse(BLOCKED_RESPONSE, true, false, List.of(), latency);
        }

        // Step 2: LLM call
        LlmRequest llmRequest = new LlmRequest(
            systemPromptConfig.text(),
            userInput,
            ragContents
        );
        LlmResponse llmResponse = llmAdapter.call(llmRequest);
        String answer = llmResponse.answer();

        // Step 3: Output leakage check
        DefenseResult outputResult = outputAnalyzer.analyzeForLeakage(answer);
        if (outputResult.blocked()) {
            long latency = System.currentTimeMillis() - start;
            log.info("Response blocked by OutputAnalyzer - reason: {}", outputResult.reason());
            return new DefendedResponse(BLOCKED_RESPONSE, true, false, List.of(), latency);
        }

        // Step 4: Refusal detection
        boolean refused = outputAnalyzer.isRefusal(answer);
        long latency = System.currentTimeMillis() - start;

        return new DefendedResponse(answer, false, refused, List.of(), latency);
    }

    /**
     * Runs only the InputAnalyzer — no LLM call.
     * Used for an early-exit before resolving RAG documents.
     */
    public DefenseResult checkInput(String userInput) {
        return inputAnalyzer.analyze(userInput);
    }

    public record DefendedResponse(
        String answer,
        boolean blocked,
        boolean refused,
        List<String> redactions,
        long latencyMs
    ) {}
}