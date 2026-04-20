package com.sentinelcore.defense.strategy;

import com.sentinelcore.defense.DefenseResult;
import com.sentinelcore.defense.InputAnalyzer;
import com.sentinelcore.defense.OutputAnalyzer;
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
public class InputOutputStrategy implements DefenseStrategy {

    private static final String BLOCKED_RESPONSE = "[blocked]";

    private final LlmAdapter llmAdapter;
    private final InputAnalyzer inputAnalyzer;
    private final OutputAnalyzer outputAnalyzer;
    private final SystemPromptBuilder systemPromptBuilder;

    @Override
    public StrategyType type() {
        return StrategyType.INPUT_OUTPUT;
    }

    @Override
    public StrategyExecutionResult execute(String userInput, Supplier<List<String>> ragContentLoader) {
        long start = System.nanoTime();
        DefenseResult inputResult = inputAnalyzer.analyze(userInput);
        if (inputResult.blocked()) {
            long latency = (System.nanoTime() - start) / 1_000_000L;
            return new StrategyExecutionResult(BLOCKED_RESPONSE, true, false, latency);
        }
        LlmResponse response = llmAdapter.call(
                new LlmRequest(systemPromptBuilder.build(), userInput, ragContentLoader.get()));
        DefenseResult outputResult = outputAnalyzer.analyzeForLeakage(response.answer());
        if (outputResult.blocked()) {
            long latency = (System.nanoTime() - start) / 1_000_000L;
            return new StrategyExecutionResult(BLOCKED_RESPONSE, true, false, latency);
        }
        boolean refused = outputAnalyzer.isRefusal(response.answer());
        long latency = (System.nanoTime() - start) / 1_000_000L;
        return new StrategyExecutionResult(response.answer(), false, refused, latency);
    }
}
