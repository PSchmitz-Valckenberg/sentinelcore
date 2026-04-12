package com.sentinelcore.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelcore.loader.dto.RagDocumentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import com.sentinelcore.exception.SeedImportException;

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
                String resourceDescription = resource.getDescription();
                try {
                    RagDocumentDto dto = objectMapper.readValue(resource.getInputStream(), RagDocumentDto.class);
                    validateDocument(dto, resourceDescription);
                    documents.add(dto);
                    log.debug("Loaded document: {} [{}]", dto.id(), dto.trustLevel());
                } catch (IOException e) {
                    throw new SeedImportException(
                            "Failed to load RAG document from resource: " + resourceDescription,
                            e
                    );
                }
            }
        } catch (IOException e) {
            throw new SeedImportException("Failed to load RAG documents from path: " + DOCUMENTS_PATH, e);
        }

        log.info("Loaded {} RAG documents total", documents.size());
        return documents;
    }

    private void validateDocument(RagDocumentDto dto, String resourceDescription) {
        if (dto.id() == null || dto.id().isBlank()) {
            throw new SeedImportException("Document id must not be blank for resource: " + resourceDescription);
        }
        if (dto.title() == null || dto.title().isBlank()) {
            throw new SeedImportException(
                    "Document title must not be blank for resource: " + resourceDescription + ", id: " + dto.id()
            );
        }
        if (dto.content() == null || dto.content().isBlank()) {
            throw new SeedImportException(
                    "Document content must not be blank for resource: " + resourceDescription + ", id: " + dto.id()
            );
        }
        if (dto.trustLevel() == null) {
            throw new SeedImportException(
                    "Document trustLevel must not be null for resource: " + resourceDescription + ", id: " + dto.id()
            );
        }
    }
}
