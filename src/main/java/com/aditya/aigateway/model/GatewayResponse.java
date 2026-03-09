package com.aditya.aigateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayResponse {

    private String id;
    private String content;
    private String modelUsed;
    private String providerUsed;
    private String tier;
    private Usage usage;
    private long latencyMs;
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        private int inputTokens;
        private int outputTokens;
        private int totalTokens;
    }
}