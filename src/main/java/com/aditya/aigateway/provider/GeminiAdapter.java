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
public class GeminiAdapter implements AiProvider {

    private final WebClient webClient;
    private final ModelRoutingConfig config;

    @Autowired
    public GeminiAdapter(WebClient.Builder webClientBuilder, ModelRoutingConfig config) {
        this.webClient = webClientBuilder.build();
        this.config = config;
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public boolean supports(String modelName) {
        return modelName != null && modelName.startsWith("gemini");
    }

    @Override
    public GatewayResponse call(GatewayRequest request, String modelName) {
        long startTime = System.currentTimeMillis();


        List<Map<String, Object>> contents = request.getMessages().stream()
                .map(m -> (Map<String, Object>) Map.of(
                        "role", m.getRole().equals("assistant") ? "model" : m.getRole(),
                        "parts", List.of(Map.of("text", m.getContent()))
                ))
                .toList();

        Map<String, Object> requestBody = Map.of("contents", contents);

        String apiKey = config.getProviders().get("gemini").getApiKey();
        String baseUrl = config.getProviders().get("gemini").getBaseUrl();
        String url = baseUrl + "/models/" + modelName + ":generateContent?key=" + apiKey;

        log.debug("Calling Google Gemini | model={}", modelName);

        Map response = webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(config.getProviders().get("gemini").getTimeoutSeconds()))
                .block();

        return parseResponse(response, modelName, startTime);
    }

    private GatewayResponse parseResponse(Map response, String modelName, long startTime) {
        List<Map> candidates = (List<Map>) response.get("candidates");
        Map content = (Map) candidates.get(0).get("content");
        List<Map> parts = (List<Map>) content.get("parts");
        String text = (String) parts.get(0).get("text");

        Map usageMetadata = (Map) response.get("usageMetadata");
        int inputTokens = (Integer) usageMetadata.get("promptTokenCount");
        int outputTokens = (Integer) usageMetadata.get("candidatesTokenCount");

        return GatewayResponse.builder()
                .id("req-" + UUID.randomUUID().toString().substring(0, 8))
                .content(text)
                .modelUsed(modelName)
                .providerUsed("gemini")
                .usage(GatewayResponse.Usage.builder()
                        .inputTokens(inputTokens)
                        .outputTokens(outputTokens)
                        .totalTokens(inputTokens + outputTokens)
                        .build())
                .latencyMs(System.currentTimeMillis() - startTime)
                .build();
    }
}