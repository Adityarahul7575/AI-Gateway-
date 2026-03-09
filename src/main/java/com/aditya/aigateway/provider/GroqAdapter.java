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
public class GroqAdapter implements AiProvider {

    private final WebClient webClient;
    private final ModelRoutingConfig config;

    @Autowired
    public GroqAdapter(WebClient.Builder webClientBuilder, ModelRoutingConfig config) {
        this.webClient = webClientBuilder.build();
        this.config = config;
    }

    @Override
    public String getProviderName() {
        return "groq";
    }

    @Override
    public boolean supports(String modelName) {
        return modelName != null && modelName.startsWith("llama");
    }

    @Override
    public GatewayResponse call(GatewayRequest request, String modelName) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", request.getMessages().stream()
                        .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                        .toList(),
                "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 1000,
                "temperature", request.getTemperature() != null ? request.getTemperature() : 0.7
        );

        String apiKey = config.getProviders().get("groq").getApiKey();
        String baseUrl = config.getProviders().get("groq").getBaseUrl();

        log.debug("Calling Groq | model={}", modelName);

        Map response = webClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(
                        config.getProviders().get("groq").getTimeoutSeconds()))
                .block();

        return parseResponse(response, modelName, startTime);
    }

    private GatewayResponse parseResponse(Map response, String modelName, long startTime) {
        List<Map> choices = (List<Map>) response.get("choices");
        Map message = (Map) choices.get(0).get("message");
        String content = (String) message.get("content");

        Map usage = (Map) response.get("usage");
        int inputTokens  = (Integer) usage.get("prompt_tokens");
        int outputTokens = (Integer) usage.get("completion_tokens");

        return GatewayResponse.builder()
                .id("req-" + UUID.randomUUID().toString().substring(0, 8))
                .content(content)
                .modelUsed(modelName)
                .providerUsed("groq")
                .usage(GatewayResponse.Usage.builder()
                        .inputTokens(inputTokens)
                        .outputTokens(outputTokens)
                        .totalTokens(inputTokens + outputTokens)
                        .build())
                .latencyMs(System.currentTimeMillis() - startTime)
                .build();
    }
}