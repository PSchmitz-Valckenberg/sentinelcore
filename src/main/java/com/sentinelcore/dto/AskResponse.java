package com.sentinelcore.dto;

import java.util.List;

public record AskResponse(
    String answer,
    boolean blocked,
    boolean refused,
    List<String> redactions,
    long latencyMs
) {}