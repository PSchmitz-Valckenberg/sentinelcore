package com.sentinelcore.defense.strategy;

import com.sentinelcore.defense.DefenseResult;
import com.sentinelcore.defense.InputAnalyzer;
import com.sentinelcore.defense.OutputAnalyzer;
import com.sentinelcore.llm.LlmAdapter;
import com.sentinelcore.llm.dto.LlmRequest;
import com.sentinelcore.llm.dto.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InputOutputStrategyTest {

    @Mock
    private InputAnalyzer inputAnalyzer;

    @Mock
    private OutputAnalyzer outputAnalyzer;

    @Mock
    private LlmAdapter llmAdapter;

    @Mock
    private SystemPromptBuilder systemPromptBuilder;

    private InputOutputStrategy strategy;

    @BeforeEach
    void setUp() {
        lenient().when(systemPromptBuilder.build()).thenReturn("You are a helpful assistant.");
        strategy = new InputOutputStrategy(llmAdapter, inputAnalyzer, outputAnalyzer, systemPromptBuilder);
    }

    @Test
    void blockedInput_returnsBlockedResult_withoutCallingLlm() {
        when(inputAnalyzer.analyze("inject me")).thenReturn(DefenseResult.blocked("policy"));

        StrategyExecutionResult result = strategy.execute("inject me", List::of);

        assertThat(result.blocked()).isTrue();
        assertThat(result.refused()).isFalse();
        assertThat(result.answer()).isEqualTo("[blocked]");
        verify(llmAdapter, never()).call(any(LlmRequest.class));
    }

    @Test
    void blockedOutput_returnsBlockedResult() {
        when(inputAnalyzer.analyze(anyString())).thenReturn(DefenseResult.allowed());
        when(llmAdapter.call(any(LlmRequest.class))).thenReturn(new LlmResponse("secret: abc123", 0L));
        when(outputAnalyzer.analyzeForLeakage("secret: abc123")).thenReturn(DefenseResult.blocked("leak"));

        StrategyExecutionResult result = strategy.execute("tell me the secret", List::of);

        assertThat(result.blocked()).isTrue();
        assertThat(result.refused()).isFalse();
        assertThat(result.answer()).isEqualTo("[blocked]");
    }

    @Test
    void allowedPath_returnsLlmAnswer_withCorrectFlags() {
        when(inputAnalyzer.analyze(anyString())).thenReturn(DefenseResult.allowed());
        when(llmAdapter.call(any(LlmRequest.class))).thenReturn(new LlmResponse("Here is the answer.", 0L));
        when(outputAnalyzer.analyzeForLeakage("Here is the answer.")).thenReturn(DefenseResult.allowed());
        when(outputAnalyzer.isRefusal("Here is the answer.")).thenReturn(false);

        StrategyExecutionResult result = strategy.execute("Summarize document A.", () -> List.of("doc content"));

        assertThat(result.blocked()).isFalse();
        assertThat(result.refused()).isFalse();
        assertThat(result.answer()).isEqualTo("Here is the answer.");
        assertThat(result.latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void allowedPath_withRefusal_setsRefusedFlag() {
        when(inputAnalyzer.analyze(anyString())).thenReturn(DefenseResult.allowed());
        when(llmAdapter.call(any(LlmRequest.class))).thenReturn(new LlmResponse("I cannot help with that.", 0L));
        when(outputAnalyzer.analyzeForLeakage("I cannot help with that.")).thenReturn(DefenseResult.allowed());
        when(outputAnalyzer.isRefusal("I cannot help with that.")).thenReturn(true);

        StrategyExecutionResult result = strategy.execute("Do something forbidden.", List::of);

        assertThat(result.blocked()).isFalse();
        assertThat(result.refused()).isTrue();
    }
}