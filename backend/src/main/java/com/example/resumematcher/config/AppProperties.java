package com.example.resumematcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String corsOrigins,
        String openaiApiKey,
        String openaiModel
) {
    public boolean hasOpenAiKey() {
        return openaiApiKey != null && !openaiApiKey.isBlank();
    }
}
