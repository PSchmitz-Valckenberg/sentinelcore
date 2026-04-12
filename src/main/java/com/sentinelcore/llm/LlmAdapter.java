package com.sentinelcore.llm;

public interface LlmAdapter {
    String ask(String systemPrompt, String userMessage);
}
