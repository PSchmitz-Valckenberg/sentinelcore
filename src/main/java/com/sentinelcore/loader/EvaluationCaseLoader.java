package com.sentinelcore.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelcore.domain.enums.EvaluationCaseType;
import com.sentinelcore.exception.InvalidCaseException;
import com.sentinelcore.loader.dto.EvaluationCaseDto;
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
public class EvaluationCaseLoader {

    private static final String CASES_PATH = "classpath:seed/cases/*.json";

    private final ObjectMapper objectMapper;

    public List<EvaluationCaseDto> loadAll() {
        List<EvaluationCaseDto> cases = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            Resource[] resources = resolver.getResources(CASES_PATH);
            for (Resource resource : resources) {
                try (java.io.InputStream inputStream = resource.getInputStream()) {
                    EvaluationCaseDto dto = objectMapper.readValue(inputStream, EvaluationCaseDto.class);
                    validate(dto);
                    cases.add(dto);
                    log.debug("Loaded case: {} [{}]", dto.id(), dto.caseType());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load evaluation cases from classpath", e);
        }

        log.info("Loaded {} evaluation cases total", cases.size());
        return cases;
    }

    private void validate(EvaluationCaseDto dto) {
        if (dto.id() == null || dto.id().isBlank()) {
            throw new InvalidCaseException("Case id must not be blank");
        }
        if (dto.caseType() == null) {
            throw new InvalidCaseException("Case caseType must not be null for case: " + dto.id());
        }
        if (dto.caseType() == EvaluationCaseType.ATTACK && dto.attackCategory() == null) {
            throw new InvalidCaseException(
                "ATTACK case must have attackCategory set. Case: " + dto.id()
            );
        }
        if (dto.userInput() == null || dto.userInput().isBlank()) {
            throw new InvalidCaseException("userInput must not be blank for case: " + dto.id());
        }
        if (dto.expectedBehavior() == null || dto.expectedBehavior().isBlank()) {
            throw new InvalidCaseException("expectedBehavior must not be blank for case: " + dto.id());
        }
    }
}
