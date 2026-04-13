package com.sentinelcore.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelcore.llm.dto.LlmRequest;
import com.sentinelcore.llm.dto.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiAdapter implements LlmAdapter {

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiAdapter(
        @Value("${sentinelcore.llm.api-key}") String apiKey,
        @Value("${sentinelcore.llm.model}") String model,
        @Value("${sentinelcore.llm.base-url}") String baseUrl,
        @Value("${sentinelcore.llm.timeout-seconds}") int timeoutSeconds,
        ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
    }

    @Override
    public LlmResponse call(LlmRequest request) {
        Map<String, Object> body = Map.of(
            "system_instruction", Map.of(
                "parts", List.of(Map.of("text", buildSystemContent(request)))
            ),
            "contents", List.of(
                Map.of("role", "user",
                       "parts", List.of(Map.of("text", request.userInput())))
            )
        );

        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            String url = baseUrl + "/models/" + model + ":generateContent";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

            long start = System.currentTimeMillis();
            HttpResponse<String> httpResponse = httpClient.send(
                httpRequest, HttpResponse.BodyHandlers.ofString()
            );
            long latencyMs = System.currentTimeMillis() - start;

            if (httpResponse.statusCode() != 200) {
                log.error("Gemini API error: status={}", httpResponse.statusCode());
                throw new LlmCallException("Gemini API returned status: " + httpResponse.statusCode());
            }

            JsonNode root = objectMapper.readTree(httpResponse.body());
            String answer = parseAnswer(root);

            log.debug("LLM call completed in {}ms", latencyMs);
            return new LlmResponse(answer, latencyMs);

        } catch (LlmCallException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmCallException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    private String parseAnswer(JsonNode root) {
        if (root == null) {
            throw new LlmCallException("Gemini response is null");
        }

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            String keys = root.fieldNames().toString();
            boolean hasError = root.has("error");
            throw new LlmCallException(
                "Gemini response missing candidates (keys=" + keys + ", hasError=" + hasError + ")"
            );
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new LlmCallException("Gemini response missing content.parts");
        }

        String text = parts.get(0).path("text").asText(null);
        if (text == null || text.isBlank()) {
            throw new LlmCallException("Gemini response has empty text in parts[0]");
        }

        return text;
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