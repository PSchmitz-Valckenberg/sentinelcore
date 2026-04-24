package com.sentinelcore.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelcore.llm.dto.LlmRequest;
import com.sentinelcore.llm.dto.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "sentinelcore.llm.provider", havingValue = "anthropic")
public class AnthropicAdapter implements LlmAdapter {

    // Messages API version. Pinned to a known-stable release rather than
    // the latest tag so evaluation runs remain reproducible across Anthropic
    // API deployments. See https://docs.anthropic.com/en/api/versioning
    private static final String ANTHROPIC_API_VERSION = "2023-06-01";

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int timeoutSeconds;
    private final int maxTokens;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AnthropicAdapter(
        @Value("${sentinelcore.llm.api-key}") String apiKey,
        @Value("${sentinelcore.llm.model}") String model,
        @Value("${sentinelcore.llm.base-url}") String baseUrl,
        @Value("${sentinelcore.llm.timeout-seconds}") int timeoutSeconds,
        @Value("${sentinelcore.llm.max-tokens:1024}") int maxTokens,
        ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.maxTokens = maxTokens;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
    }

    @Override
    public LlmResponse call(LlmRequest request) {
        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "system", buildSystemContent(request),
            "messages", List.of(
                Map.of("role", "user", "content", request.userInput())
            )
        );

        try {
            String bodyJson = objectMapper.writeValueAsString(body);
            String url = baseUrl + "/messages";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_API_VERSION)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

            long start = System.currentTimeMillis();
            HttpResponse<String> httpResponse = httpClient.send(
                httpRequest, HttpResponse.BodyHandlers.ofString()
            );
            long latencyMs = System.currentTimeMillis() - start;

            if (httpResponse.statusCode() != 200) {
                String details = describeErrorBody(httpResponse.body());
                log.error("Anthropic API error: status={} details={}", httpResponse.statusCode(), details);
                throw new LlmCallException(
                    "Anthropic API returned status " + httpResponse.statusCode() + ": " + details
                );
            }

            JsonNode root = objectMapper.readTree(httpResponse.body());
            String answer = parseAnswer(root);

            log.debug("LLM call completed in {}ms", latencyMs);
            return new LlmResponse(answer, latencyMs);

        } catch (LlmCallException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmCallException("Failed to call Anthropic API: " + e.getMessage(), e);
        }
    }

    // Package-private so unit tests can exercise parsing without hitting the network.
    String parseAnswer(JsonNode root) {
        if (root == null) {
            throw new LlmCallException("Anthropic response is null");
        }

        JsonNode content = root.path("content");
        if (!content.isArray() || content.isEmpty()) {
            throw new LlmCallException(
                "Anthropic response missing content: " + describeErrorNode(root)
            );
        }

        // Anthropic returns a list of content blocks. For a plain text response
        // we expect the first block to be of type "text". Tool-use blocks are
        // not in scope for SentinelCore's evaluation pipeline.
        JsonNode firstBlock = content.get(0);
        String text = firstBlock.path("text").asText(null);
        if (text == null || text.isBlank()) {
            throw new LlmCallException("Anthropic response has empty text in content[0]");
        }

        return text;
    }

    /**
     * Best-effort extraction of the Anthropic error envelope from a raw response
     * body. Falls back to a safely truncated body snippet if the payload is not
     * JSON or has no recognisable error shape — the goal is always to leave
     * *something* actionable in the exception/log rather than just a status code.
     */
    private String describeErrorBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "(empty body)";
        }
        try {
            return describeErrorNode(objectMapper.readTree(rawBody));
        } catch (Exception e) {
            return "unparseable body: " + truncate(rawBody, 500);
        }
    }

    private String describeErrorNode(JsonNode root) {
        JsonNode error = root.path("error");
        if (error.isObject() && !error.isEmpty()) {
            String type = error.path("type").asText("unknown");
            String message = error.path("message").asText("(no message)");
            return "error.type=" + type + ", error.message=" + truncate(message, 500);
        }
        List<String> keys = new ArrayList<>();
        root.fieldNames().forEachRemaining(keys::add);
        return "no error field; top-level keys=" + keys;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "(null)";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
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
