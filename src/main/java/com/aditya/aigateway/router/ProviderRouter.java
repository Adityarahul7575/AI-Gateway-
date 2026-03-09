package com.aditya.aigateway.router;

import com.aditya.aigateway.config.ModelRoutingConfig;
import com.aditya.aigateway.exception.GatewayException;
import com.aditya.aigateway.model.GatewayRequest;
import com.aditya.aigateway.provider.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ProviderRouter {
    private final List<AiProvider> providers;
    private final ModelRoutingConfig config;

    public ProviderRouter(List<AiProvider> providers,ModelRoutingConfig config){
        this.providers = providers;
        this.config = config;
    }

    //decide which model to route

    //task priority higher

    public String resolveModel(GatewayRequest request) {
        if (request.getTask() != null) {
            String model = config.getTaskModelMap().get(request.getTask());
            if (model != null) return model;
            throw new GatewayException(
                    "Unknown task: " + request.getTask() + ". Valid values: chat, code, summarize, analyze",
                    HttpStatus.BAD_REQUEST);
        }

        if (request.getTier() != null) {
            String model = config.getTierModelMap().get(request.getTier());
            if (model != null) return model;
            throw new GatewayException(
                    "Unknown tier: " + request.getTier() + ". Valid values: fast, smart, cheap",
                    HttpStatus.BAD_REQUEST);
        }

        //default fallback
        log.debug("No tier/task matched, using default model");
        return config.getDefaultModel();

    }

        // Finds the right adapter for the resolved model
    public AiProvider route (String modelName){
            return providers.stream()
                    .filter(p -> p.supports(modelName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No provider found for model: " + modelName));
    }

}
