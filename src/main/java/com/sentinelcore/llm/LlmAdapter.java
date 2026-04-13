package com.sentinelcore.llm;

import com.sentinelcore.llm.dto.LlmRequest;
import com.sentinelcore.llm.dto.LlmResponse;

public interface LlmAdapter {
    LlmResponse call(LlmRequest request);
}
