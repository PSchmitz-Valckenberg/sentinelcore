package com.sentinelcore.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parsing-level unit tests for AnthropicAdapter.
 *
 * These tests exercise the JSON parsing path directly without touching the
 * network. Full end-to-end behaviour of the Anthropic Messages API is not
 * covered here — any regression in the on-the-wire contract would surface
 * either in manual smoke tests or in a future integration test backed by
 * a recorded fixture.
 */
class AnthropicAdapterTest {

    private ObjectMapper objectMapper;
    private AnthropicAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new AnthropicAdapter(
            "test-key",
            "claude-haiku-4-5-20251001",
            "https://api.anthropic.com/v1",
            30,
            1024,
            objectMapper
        );
    }

    @Test
    @DisplayName("parseAnswer extracts text from the first content block on a well-formed response")
    void parseAnswerHappyPath() throws Exception {
        JsonNode root = objectMapper.readTree("""
            {
              "id": "msg_01",
              "type": "message",
              "role": "assistant",
              "content": [
                { "type": "text", "text": "Document A discusses polar ice caps." }
              ],
              "model": "claude-haiku-4-5-20251001",
              "stop_reason": "end_turn"
            }
            """);

        assertThat(adapter.parseAnswer(root)).isEqualTo("Document A discusses polar ice caps.");
    }

    @Test
    @DisplayName("parseAnswer throws LlmCallException when the response is null")
    void parseAnswerRejectsNull() {
        assertThatThrownBy(() -> adapter.parseAnswer(null))
            .isInstanceOf(LlmCallException.class)
            .hasMessageContaining("null");
    }

    @Test
    @DisplayName("parseAnswer throws LlmCallException when the content array is missing")
    void parseAnswerRejectsMissingContent() throws Exception {
        JsonNode root = objectMapper.readTree("""
            { "id": "msg_01", "type": "message", "role": "assistant" }
            """);

        assertThatThrownBy(() -> adapter.parseAnswer(root))
            .isInstanceOf(LlmCallException.class)
            .hasMessageContaining("missing content");
    }

    @Test
    @DisplayName("parseAnswer surfaces Anthropic error type and message in the exception")
    void parseAnswerIncludesErrorTypeAndMessageForApiErrors() throws Exception {
        JsonNode root = objectMapper.readTree("""
            { "type": "error", "error": { "type": "invalid_request_error", "message": "Bad request" } }
            """);

        assertThatThrownBy(() -> adapter.parseAnswer(root))
            .isInstanceOf(LlmCallException.class)
            .hasMessageContaining("error.type=invalid_request_error")
            .hasMessageContaining("error.message=Bad request");
    }

    @Test
    @DisplayName("parseAnswer throws LlmCallException when the content array is empty")
    void parseAnswerRejectsEmptyContentArray() throws Exception {
        JsonNode root = objectMapper.readTree("""
            { "id": "msg_01", "type": "message", "content": [] }
            """);

        assertThatThrownBy(() -> adapter.parseAnswer(root))
            .isInstanceOf(LlmCallException.class)
            .hasMessageContaining("missing content");
    }

    @Test
    @DisplayName("parseAnswer throws LlmCallException when the first content block has no text field")
    void parseAnswerRejectsMissingText() throws Exception {
        JsonNode root = objectMapper.readTree("""
            {
              "id": "msg_01",
              "content": [ { "type": "text" } ]
            }
            """);

        assertThatThrownBy(() -> adapter.parseAnswer(root))
            .isInstanceOf(LlmCallException.class)
            .hasMessageContaining("empty text");
    }

    @Test
    @DisplayName("parseAnswer throws LlmCallException when the first content block has a blank text value")
    void parseAnswerRejectsBlankText() throws Exception {
        JsonNode root = objectMapper.readTree("""
            {
              "id": "msg_01",
              "content": [ { "type": "text", "text": "   " } ]
            }
            """);

        assertThatThrownBy(() -> adapter.parseAnswer(root))
            .isInstanceOf(LlmCallException.class)
            .hasMessageContaining("empty text");
    }
}
