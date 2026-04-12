package com.sentinelcore.loader;

import com.sentinelcore.domain.entity.EvaluationCase;
import com.sentinelcore.domain.entity.RagDocument;
import com.sentinelcore.domain.enums.CheckType;
import com.sentinelcore.exception.SeedImportException;
import com.sentinelcore.loader.dto.EvaluationCaseDto;
import com.sentinelcore.loader.dto.RagDocumentDto;
import com.sentinelcore.repository.EvaluationCaseRepository;
import com.sentinelcore.repository.RagDocumentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeedImporter implements ApplicationRunner {

    private final RagDocumentLoader ragDocumentLoader;
    private final EvaluationCaseLoader evaluationCaseLoader;
    private final RagDocumentRepository ragDocumentRepository;
    private final EvaluationCaseRepository evaluationCaseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting seed import...");

        boolean hasDocuments = ragDocumentRepository.count() > 0;
        boolean hasCases = evaluationCaseRepository.count() > 0;

        if (hasDocuments && hasCases) {
            log.info("Seed data already present — skipping import.");
            return;
        }

        if (!hasDocuments) {
            importDocuments();
        } else {
            log.info("RAG documents already present — skipping document import.");
        }

        if (!hasCases) {
            importCases();
        } else {
            log.info("Evaluation cases already present — skipping case import.");
        }

        log.info("Seed import completed successfully.");
    }

    private void importDocuments() {
        List<RagDocumentDto> dtos = ragDocumentLoader.loadAll();
        List<RagDocument> entities = dtos.stream()
            .map(dto -> RagDocument.builder()
                .id(dto.id())
                .title(dto.title())
                .content(dto.content())
                .trustLevel(dto.trustLevel())
                .build())
            .toList();
        List<RagDocument> saved = ragDocumentRepository.saveAll(entities);
        log.info("Imported {} RAG documents.", saved.size());
    }

    private void importCases() {
        Map<String, RagDocument> documentIndex = ragDocumentRepository.findAll()
            .stream()
            .collect(Collectors.toMap(RagDocument::getId, Function.identity()));

        List<EvaluationCaseDto> dtos = evaluationCaseLoader.loadAll();
        List<EvaluationCase> entities = dtos.stream()
            .map(dto -> buildCase(dto, documentIndex))
            .toList();
        List<EvaluationCase> saved = evaluationCaseRepository.saveAll(entities);
        log.info("Imported {} evaluation cases.", saved.size());
    }

    private EvaluationCase buildCase(EvaluationCaseDto dto, Map<String, RagDocument> documentIndex) {
        Set<RagDocument> ragDocs = new HashSet<>();
        if (dto.ragDocumentIds() != null) {
            for (String docId : dto.ragDocumentIds()) {
                RagDocument doc = documentIndex.get(docId);
                if (doc == null) {
                    throw new SeedImportException(
                        "Case [" + dto.id() + "] references unknown document id: " + docId
                    );
                }
                ragDocs.add(doc);
            }
        }

        Set<CheckType> checks = dto.relevantChecks() != null
            ? new HashSet<>(dto.relevantChecks())
            : new HashSet<>();

        return EvaluationCase.builder()
            .id(dto.id())
            .caseType(dto.caseType())
            .attackCategory(dto.attackCategory())
            .name(dto.name())
            .userInput(dto.userInput())
            .expectedBehavior(dto.expectedBehavior())
            .ragDocuments(ragDocs)
            .relevantChecks(checks)
            .build();
    }
}