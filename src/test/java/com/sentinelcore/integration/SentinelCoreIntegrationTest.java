package com.sentinelcore.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.sentinelcore.llm.LlmAdapter;
import com.sentinelcore.llm.dto.LlmResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the full evaluation pipeline.
 *
 * Boots the full Spring context against a disposable PostgreSQL instance
 * managed by Testcontainers — no local database required. The LlmAdapter
 * is mocked to keep the test hermetic.
 *
 * Requires a running Docker daemon on the host (or a configured
 * DOCKER_HOST). CI is expected to provide Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class SentinelCoreIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    LlmAdapter llmAdapter;

    @Test
    @DisplayName("Full pipeline: create run -> execute -> results -> report")
    void fullEvaluationPipeline() throws Exception {
        // Mock LLM to return a benign answer - no canary, no injection compliance
        when(llmAdapter.call(any())).thenReturn(
                new LlmResponse("This document covers climate research findings.", 350L));

        // Step 1: Create run
        MvcResult createResult = mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "BASELINE",
                                  "model": "gemini-2.0-flash"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").exists())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        String runId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("runId").asText();
        assertThat(runId).startsWith("run-");

        // Step 2: Execute run
        mockMvc.perform(post("/api/runs/{id}/execute", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalCases").isNumber())
                .andExpect(jsonPath("$.completedCases").isNumber());

        // Step 3: Get results
        MvcResult resultsResult = mockMvc.perform(get("/api/runs/{id}/results", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.executions").isArray())
                .andReturn();

        JsonNode results = objectMapper.readTree(resultsResult.getResponse().getContentAsString());
        assertThat(results.get("executions").size()).isGreaterThan(0);

        // Step 4: Get report
        mockMvc.perform(get("/api/runs/{id}/report", runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.metrics").exists())
                .andExpect(jsonPath("$.metrics.attackSuccessRate").isNumber())
                .andExpect(jsonPath("$.metrics.falsePositiveRate").isNumber())
                .andExpect(jsonPath("$.metrics.refusalRate").isNumber())
                .andExpect(jsonPath("$.metrics.avgLatencyMs").isNumber())
                .andExpect(jsonPath("$.breakdown").exists());
    }

    @Test
    @DisplayName("GET /api/runs/{id}/results returns an error for unknown runId")
    void unknownRunIdReturnsError() throws Exception {
        mockMvc.perform(get("/api/runs/run-does-not-exist/results"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/ask returns 200 with answer field")
    void askEndpointReturnsAnswer() throws Exception {
        when(llmAdapter.call(any())).thenReturn(
                new LlmResponse("Document A discusses polar ice caps.", 280L));

        mockMvc.perform(post("/api/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userInput": "What is document A about?",
                                  "ragDocumentIds": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Document A discusses polar ice caps."))
                .andExpect(jsonPath("$.blocked").value(false))
                .andExpect(jsonPath("$.refused").value(false));
    }
}
