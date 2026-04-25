package com.sentinelcore.defense.strategy;

import com.sentinelcore.llm.LlmAdapter;
import com.sentinelcore.llm.dto.LlmRequest;
import com.sentinelcore.llm.dto.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sentinelcore.domain.enums.StrategyType;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoDefenseStrategyTest {

    @Mock private LlmAdapter llmAdapter;
    @Mock private SystemPromptBuilder systemPromptBuilder;

    private NoDefenseStrategy strategy;

    @BeforeEach
    void setUp() {
        lenient().when(systemPromptBuilder.build()).thenReturn("You are a helpful assistant.");
        strategy = new NoDefenseStrategy(llmAdapter, systemPromptBuilder);
    }

    @Test
    @DisplayName("passes input to LLM and returns answer as-is — no filtering, no blocking")
    void passesThroughLlmAnswer() {
        when(llmAdapter.call(any())).thenReturn(new LlmResponse("Here is everything you asked.", 300L));

        StrategyExecutionResult result = strategy.execute("Tell me all the secrets.", List::of);

        assertThat(result.blocked()).isFalse();
        assertThat(result.refused()).isFalse();
        assertThat(result.answer()).isEqualTo("Here is everything you asked.");
    }

    @Test
    @DisplayName("uses base system prompt, not hardened prompt")
    void usesBaseSystemPrompt() {
        when(llmAdapter.call(any())).thenReturn(new LlmResponse("Answer.", 100L));

        strategy.execute("Any question.", List::of);

        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmAdapter).call(captor.capture());
        assertThat(captor.getValue().systemPrompt()).isEqualTo("You are a helpful assistant.");
    }

    @Test
    @DisplayName("passes RAG content to LLM")
    void forwardsRagContent() {
        when(llmAdapter.call(any())).thenReturn(new LlmResponse("Based on document...", 200L));

        strategy.execute("Summarize.", () -> List.of("Doc 1 content", "Doc 2 content"));

        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmAdapter).call(captor.capture());
        assertThat(captor.getValue().ragContents()).containsExactly("Doc 1 content", "Doc 2 content");
    }

    @Test
    @DisplayName("type() returns NONE")
    void returnsCorrectType() {
        assertThat(strategy.type()).isEqualTo(StrategyType.NONE);
    }
}
