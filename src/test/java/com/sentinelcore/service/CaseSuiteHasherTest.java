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
    void hashIsLowerHex64Chars() {
        List<EvaluationCase> cases = List.of(caseWith("CASE-001", "test"));
        String hash = hasher.compute(cases);
        // SHA-256 always produces exactly 64 lowercase hex characters
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void separatorAmbiguityDoesNotProduceCollision() {
        // Without length-prefix encoding these two would hash to the same payload:
        // id="A:B", input="C"  ->  "A:B:C"
        // id="A",   input="B:C" ->  "A:B:C"
        EvaluationCase case1 = caseWith("A:B", "C");
        EvaluationCase case2 = caseWith("A", "B:C");

        assertThat(hasher.compute(List.of(case1)))
                .isNotEqualTo(hasher.compute(List.of(case2)));
    }

    private EvaluationCase caseWith(String id, String userInput) {
        EvaluationCase ec = new EvaluationCase();
        ec.setId(id);
        ec.setUserInput(userInput);
        return ec;
    }
}