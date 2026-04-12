package com.sentinelcore.controller;

import com.sentinelcore.dto.AskRequest;
import com.sentinelcore.dto.AskResponse;
import com.sentinelcore.llm.LlmAdapter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AskController {

    private final LlmAdapter llmAdapter;

    public AskController(LlmAdapter llmAdapter) {
        this.llmAdapter = llmAdapter;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        String llmResponse = llmAdapter.ask(request.systemPrompt(), request.userMessage());
        return ResponseEntity.ok(new AskResponse(
            request.systemPrompt(),
            request.userMessage(),
            llmResponse
        ));
    }
}
