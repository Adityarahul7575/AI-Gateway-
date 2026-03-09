package com.aditya.aigateway.provider;

import com.aditya.aigateway.config.ModelRoutingConfig;
import com.aditya.aigateway.model.GatewayRequest;
import com.aditya.aigateway.model.GatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class ClaudeAdapter implements AiProvider {

    private final WebClient webClient;
    private final ModelRoutingConfig config;

    @Autowired
    public ClaudeAdapter(WebClient.Builder webClientBuilder, ModelRoutingConfig config) {
        this.webClient = webClientBuilder.build();
        this.config = config;
    }

    @Override
    public String getProviderName() {
        return "anthropic";
    }

    @Override
    public boolean supports(String modelName) {
        return modelName != null && modelName.startsWith("claude");
    }

    @Override
    public GatewayResponse call(GatewayRequest request, String modelName) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 1000,
                "messages", request.getMessages().stream()
                        .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                        .toList()
        );

        String apiKey = config.getProviders().get("anthropic").getApiKey();
        String baseUrl = config.getProviders().get("anthropic").getBaseUrl();

        log.debug("Calling Anthropic Claude | model={}", modelName);

        Map response = webClient.post()
                .uri(baseUrl + "/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(config.getProviders().get("anthropic").getTimeoutSeconds()))
                .block();

        return parseResponse(response, modelName, startTime);
    }

    private GatewayResponse parseResponse(Map response, String modelName, long startTime) {
        List<Map> content = (List<Map>) response.get("content");
        String text = (String) content.get(0).get("text");

        Map usage = (Map) response.get("usage");
        int inputTokens = (Integer) usage.get("input_tokens");
        int outputTokens = (Integer) usage.get("output_tokens");

        return GatewayResponse.builder()
                .id("req-" + UUID.randomUUID().toString().substring(0, 8))
                .content(text)
                .modelUsed(modelName)
                .providerUsed("anthropic")
                .usage(GatewayResponse.Usage.builder()
                        .inputTokens(inputTokens)
                        .outputTokens(outputTokens)
                        .totalTokens(inputTokens + outputTokens)
                        .build())
                .latencyMs(System.currentTimeMillis() - startTime)
                .build();
    }
}