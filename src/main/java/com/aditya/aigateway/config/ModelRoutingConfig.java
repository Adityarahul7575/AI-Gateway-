package com.aditya.aigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Data                           // ← generates all getters/setters
@ConfigurationProperties(prefix = "gateway")
public class ModelRoutingConfig {

    private Map<String, ProviderConfig> providers;
    private Map<String, String> tierModelMap;
    private Map<String, String> taskModelMap;
    private String defaultModel;

    private Map<String, String> apiKeys; // AUTH FILTER
    private RateLimitConfig rateLimit; //rateLimiter

    @Data
    public static class ProviderConfig {
        private String apiKey;
        private String baseUrl;
        private int timeoutSeconds;
    }

    @Data
    public static class RateLimitConfig{
        private int requestsPerMinute;
        private int burstCapacity;
    }
}