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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
 * Uses @SpringBootTest to load the full application context with a real
 * PostgreSQL instance provided by Testcontainers (configured via application-test.yml).
 * The LlmAdapter is mocked to avoid real API calls.
 *
 * This test verifies:
 * - A run can be created via POST /api/runs
 * - The run can be executed via POST /api/runs/{id}/execute
 * - Results are retrievable via GET /api/runs/{id}/results
 * - Metrics are retrievable via GET /api/runs/{id}/report
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SentinelCoreIntegrationTest {

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
    @DisplayName("GET /api/runs/{id}/results returns 404 for unknown runId")
    void unknownRunIdReturns404() throws Exception {
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
