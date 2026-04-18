package com.sentinelcore.defense.strategy;

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
public class NoDefenseStrategy implements DefenseStrategy {

    private final LlmAdapter llmAdapter;
    private final SystemPromptBuilder systemPromptBuilder;

    @Override
    public StrategyType type() {
        return StrategyType.NONE;
    }

    @Override
    public StrategyExecutionResult execute(String userInput, Supplier<List<String>> ragContentLoader) {
        long start = System.nanoTime();
        LlmResponse response = llmAdapter.call(
            new LlmRequest(systemPromptBuilder.build(), userInput, ragContentLoader.get()));
        long latency = (System.nanoTime() - start) / 1_000_000L;
        return new StrategyExecutionResult(response.answer(), false, false, latency);
    }
}