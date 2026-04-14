package com.sentinelcore.controller;

import com.sentinelcore.defense.DefenseService;
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
    private final DefenseService defenseService;

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

        return ResponseEntity.ok(new AskResponse(
            llmResponse.answer(),
            false,
            false,
            List.of(),
            llmResponse.latencyMs()
        ));
    }

    @PostMapping("/ask-defended")
    public ResponseEntity<AskResponse> askDefended(@Valid @RequestBody AskRequest request) {
        DefenseService.DefendedResponse initialResult = defenseService.process(request.userInput(), List.of());
        if (initialResult.blocked()) {
            log.debug("Defended request processed - blocked={}, refused={}, latency={}ms",
                initialResult.blocked(), initialResult.refused(), initialResult.latencyMs());

            return ResponseEntity.ok(new AskResponse(
                initialResult.answer(),
                initialResult.blocked(),
                initialResult.refused(),
                initialResult.redactions(),
                initialResult.latencyMs()
            ));
        }

        List<String> ragContents = resolveRagDocuments(request.ragDocumentIds());
        DefenseService.DefendedResponse result = defenseService.process(request.userInput(), ragContents);
        log.debug("Defended request processed - blocked={}, refused={}, latency={}ms",
            result.blocked(), result.refused(), result.latencyMs());

        return ResponseEntity.ok(new AskResponse(
            result.answer(),
            result.blocked(),
            result.refused(),
            result.redactions(),
            result.latencyMs()
        ));
    }

    private List<String> resolveRagDocuments(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ragDocumentRepository.findAllById(ids)
            .stream()
            .map(doc -> doc.getTitle() + "\n" + doc.getContent())
            .toList();
    }
}