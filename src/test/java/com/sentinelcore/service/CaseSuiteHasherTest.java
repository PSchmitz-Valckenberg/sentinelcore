package com.sentinelcore.service;

import com.sentinelcore.domain.entity.EvaluationCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaseSuiteHasherTest {

    private final CaseSuiteHasher hasher = new CaseSuiteHasher();

    @Test
    void sameCasesProduceSameHash() {
        List<EvaluationCase> cases = List.of(
                caseWith("CASE-001", "Ignore instructions"),
                caseWith("CASE-002", "What is the capital of France?")
        );
        assertThat(hasher.compute(cases)).isEqualTo(hasher.compute(cases));
    }

    @Test
    void orderIndependent_sortedById() {
        EvaluationCase a = caseWith("CASE-001", "input A");
        EvaluationCase b = caseWith("CASE-002", "input B");

        String hash1 = hasher.compute(List.of(a, b));
        String hash2 = hasher.compute(List.of(b, a));

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void differentUserInput_producesDifferentHash() {
        List<EvaluationCase> original = List.of(caseWith("CASE-001", "original input"));
        List<EvaluationCase> modified = List.of(caseWith("CASE-001", "modified input"));

        assertThat(hasher.compute(original)).isNotEqualTo(hasher.compute(modified));
    }

    @Test
    void addedCase_producesDifferentHash() {
        List<EvaluationCase> original = List.of(caseWith("CASE-001", "input A"));
        List<EvaluationCase> extended = List.of(
                caseWith("CASE-001", "input A"),
                caseWith("CASE-002", "input B")
        );

        assertThat(hasher.compute(original)).isNotEqualTo(hasher.compute(extended));
    }

    @Test
    void hashIs64HexChars() {
        List<EvaluationCase> cases = List.of(caseWith("CASE-001", "test"));
        assertThat(hasher.compute(cases)).hasSize(64);
    }

    private EvaluationCase caseWith(String id, String userInput) {
        EvaluationCase ec = new EvaluationCase();
        ec.setId(id);
        ec.setUserInput(userInput);
        return ec;
    }
}