package com.sentinelcore.controller;

import com.sentinelcore.domain.entity.EvaluationRun;
import com.sentinelcore.domain.enums.RunMode;
import com.sentinelcore.domain.enums.RunStatus;
import com.sentinelcore.domain.enums.StrategyType;
import com.sentinelcore.repository.AttackExecutionRepository;
import com.sentinelcore.repository.EvaluationCaseRepository;
import com.sentinelcore.service.EvaluationRunService;
import com.sentinelcore.service.ReportingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RunController.class)
class RunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluationRunService runService;

    @MockBean
    private AttackExecutionRepository executionRepository;

    @MockBean
    private EvaluationCaseRepository caseRepository;

    @MockBean
    private ReportingService reportingService;

    @Test
    void createRun_withoutStrategyType_passesNullToService() throws Exception {
        EvaluationRun stubRun = buildStubRun(RunMode.DEFENDED, StrategyType.INPUT_OUTPUT);
        when(runService.createRun(eq(RunMode.DEFENDED), eq("gemini-2.0-flash"), isNull()))
                .thenReturn(stubRun);

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode":"DEFENDED","model":"gemini-2.0-flash"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value("run-test-1"))
                .andExpect(jsonPath("$.status").value("CREATED"));

        verify(runService).createRun(RunMode.DEFENDED, "gemini-2.0-flash", null);
    }

    @Test
    void createRun_withExplicitStrategyType_forwardsItUnchanged() throws Exception {
        EvaluationRun stubRun = buildStubRun(RunMode.DEFENDED, StrategyType.PROMPT_HARDENING);
        when(runService.createRun(eq(RunMode.DEFENDED), eq("gemini-2.0-flash"), eq(StrategyType.PROMPT_HARDENING)))
                .thenReturn(stubRun);

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode":"DEFENDED","model":"gemini-2.0-flash","strategyType":"PROMPT_HARDENING"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value("run-test-1"))
                .andExpect(jsonPath("$.status").value("CREATED"));

        verify(runService).createRun(RunMode.DEFENDED, "gemini-2.0-flash", StrategyType.PROMPT_HARDENING);
    }

    @Test
    void createRun_baselineWithoutStrategyType_passesNullToService() throws Exception {
        EvaluationRun stubRun = buildStubRun(RunMode.BASELINE, StrategyType.NONE);
        when(runService.createRun(eq(RunMode.BASELINE), eq("gemini-2.0-flash"), isNull()))
                .thenReturn(stubRun);

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode":"BASELINE","model":"gemini-2.0-flash"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value("run-test-1"))
                .andExpect(jsonPath("$.status").value("CREATED"));

        verify(runService).createRun(RunMode.BASELINE, "gemini-2.0-flash", null);
    }

    private EvaluationRun buildStubRun(RunMode mode, StrategyType strategyType) {
        EvaluationRun run = new EvaluationRun();
        run.setId("run-test-1");
        run.setMode(mode);
        run.setModel("gemini-2.0-flash");
        run.setStrategyType(strategyType);
        run.setStatus(RunStatus.CREATED);
        run.setCreatedAt(Instant.now());
        return run;
    }
}