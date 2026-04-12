package com.sentinelcore.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelcore.loader.dto.RagDocumentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagDocumentLoader {

    private static final String DOCUMENTS_PATH = "classpath:seed/documents/*.json";

    private final ObjectMapper objectMapper;

    public List<RagDocumentDto> loadAll() {
        List<RagDocumentDto> documents = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources(DOCUMENTS_PATH);
            for (Resource resource : resources) {
                RagDocumentDto dto = objectMapper.readValue(resource.getInputStream(), RagDocumentDto.class);
                validateDocument(dto);
                documents.add(dto);
                log.debug("Loaded document: {} [{}]", dto.id(), dto.trustLevel());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load RAG documents from classpath", e);
        }

        log.info("Loaded {} RAG documents total", documents.size());
        return documents;
    }

    private void validateDocument(RagDocumentDto dto) {
        if (dto.id() == null || dto.id().isBlank()) {
            throw new IllegalArgumentException("Document id must not be blank");
        }
        if (dto.title() == null || dto.title().isBlank()) {
            throw new IllegalArgumentException("Document title must not be blank for: " + dto.id());
        }
        if (dto.content() == null || dto.content().isBlank()) {
            throw new IllegalArgumentException("Document content must not be blank for: " + dto.id());
        }
        if (dto.trustLevel() == null) {
            throw new IllegalArgumentException("Document trustLevel must not be null for: " + dto.id());
        }
    }
}
