package com.sentinelcore.controller;

import com.sentinelcore.domain.config.SystemPromptConfig;
import com.sentinelcore.dto.AskRequest;
import com.sentinelcore.dto.AskResponse;
import com.sentinelcore.llm.LlmAdapter;
import com.sentinelcore.llm.dto.LlmRequest;
import com.sentinelcore.llm.dto.LlmResponse;
import com.sentinelcore.repository.RagDocumentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AskController {

    private final LlmAdapter llmAdapter;
    private final SystemPromptConfig systemPromptConfig;
    private final RagDocumentRepository ragDocumentRepository;

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        List<String> ragContents = resolveRagDocuments(request.ragDocumentIds());

        LlmRequest llmRequest = new LlmRequest(
            systemPromptConfig.text(),
            request.userInput(),
            ragContents
        );

        LlmResponse llmResponse = llmAdapter.call(llmRequest);
        log.debug("LLM answered in {}ms", llmResponse.latencyMs());

        return ResponseEntity.ok(new AskResponse(llmResponse.answer(), llmResponse.latencyMs()));
    }

    private List<String> resolveRagDocuments(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ragDocumentRepository.findAllById(ids)
            .stream()
            .map(doc -> doc.getTitle() + "\n" + doc.getContent())
            .toList();
    }
}