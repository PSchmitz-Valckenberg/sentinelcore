package com.sentinelcore.domain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sentinelcore.system-prompt")
public record SystemPromptConfig(String text, String canaryToken) {}