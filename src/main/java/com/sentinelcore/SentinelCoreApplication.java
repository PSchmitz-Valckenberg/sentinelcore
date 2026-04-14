package com.sentinelcore;

import com.sentinelcore.defense.DefenseConfig;
import com.sentinelcore.domain.config.SystemPromptConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({SystemPromptConfig.class, DefenseConfig.class})
public class SentinelCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(SentinelCoreApplication.class, args);
    }
}