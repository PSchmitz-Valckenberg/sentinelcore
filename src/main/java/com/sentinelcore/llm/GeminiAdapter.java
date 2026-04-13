package com.sentinelcore.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelcore.llm.dto.LlmRequest;
import com.sentinelcore.llm.dto.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiAdapter implements LlmAdapter {

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public GeminiAdapter(
            RestClient.Builder builder,
            @Value("${llm.api-key}") String apiKey,
            ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("null")
    public LlmResponse call(LlmRequest request) {
        String systemPrompt = buildSystemContent(request);
        String userMessage = request.userInput();

        Map<String, Object> body = Map.of(
            "system_instruction", Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
            ),
            "contents", List.of(
                Map.of(
                    "parts", List.of(Map.of("text", userMessage))
                )
            )
        );

        try {
            String bodyJson = objectMapper.writeValueAsString(body);

            long start = System.currentTimeMillis();
            String responseBody = restClient.post()
                .uri(GEMINI_URL + "?key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyJson)
                .retrieve()
                .body(String.class);
            long latencyMs = System.currentTimeMillis() - start;

            JsonNode root = objectMapper.readTree(responseBody);
            String answer = root
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text")
                .asText();

            log.debug("LLM call completed in {}ms", latencyMs);
            return new LlmResponse(answer, latencyMs);
        } catch (Exception e) {
            throw new LlmCallException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    private String buildSystemContent(LlmRequest request) {
        if (request.ragContents() == null || request.ragContents().isEmpty()) {
            return request.systemPrompt();
        }

        StringBuilder sb = new StringBuilder(request.systemPrompt());
        sb.append("\n\n--- Retrieved Documents ---\n");
        for (int i = 0; i < request.ragContents().size(); i++) {
            sb.append("[Document ").append(i + 1).append("]\n");
            sb.append(request.ragContents().get(i));
            sb.append("\n\n");
        }
        return sb.toString();
    }
}
