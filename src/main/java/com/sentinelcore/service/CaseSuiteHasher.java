package com.sentinelcore.service;

import com.sentinelcore.domain.entity.EvaluationCase;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Computes a deterministic SHA-256 fingerprint over the active case suite.
 * The fingerprint captures case IDs and user inputs sorted by ID, so any
 * change to case content or set membership produces a different hash.
 */
@Component
public class CaseSuiteHasher {

    public String compute(List<EvaluationCase> cases) {
        // Sort by ID for determinism — insertion order is not guaranteed.
        String payload = cases.stream()
                .sorted(Comparator.comparing(EvaluationCase::getId))
                .map(c -> c.getId() + ":" + c.getUserInput())
                .collect(Collectors.joining("|"));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present in every JVM.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}