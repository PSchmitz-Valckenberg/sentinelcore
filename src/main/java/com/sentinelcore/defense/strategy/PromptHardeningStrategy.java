package com.sentinelcore.defense.strategy;

import com.sentinelcore.defense.OutputAnalyzer;
import com.sentinelcore.defense.DefenseResult;
import com.sentinelcore.domain.enums.StrategyType;
import com.sentinelcore.llm.LlmAdapter;
import com.sentinelcore.llm.dto.LlmRequest;
import com.sentinelcore.llm.dto.LlmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class PromptHardeningStrategy implements DefenseStrategy {

    private static final String BLOCKED_RESPONSE = "[blocked]";

    private final LlmAdapter llmAdapter;
    private final OutputAnalyzer outputAnalyzer;
    private final SystemPromptBuilder systemPromptBuilder;

    @Override
    public StrategyType type() {
        return StrategyType.PROMPT_HARDENING;
    }

    @Override
    public StrategyExecutionResult execute(String userInput, Supplier<List<String>> ragContentLoader) {
        long start = System.nanoTime();
        LlmResponse response = llmAdapter.call(
                new LlmRequest(systemPromptBuilder.buildHardened(), userInput, ragContentLoader.get()));
        DefenseResult outputResult = outputAnalyzer.analyzeForLeakage(response.answer());
        if (outputResult.blocked()) {
            long latency = (System.nanoTime() - start) / 1_000_000L;
            return new StrategyExecutionResult(BLOCKED_RESPONSE, true, false, latency);
        }
        long latency = (System.nanoTime() - start) / 1_000_000L;
        boolean refused = outputAnalyzer.isRefusal(response.answer());
        return new StrategyExecutionResult(response.answer(), false, refused, latency);
    }
}
