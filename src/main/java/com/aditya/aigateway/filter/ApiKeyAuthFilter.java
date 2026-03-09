package com.aditya.aigateway.filter;


import com.aditya.aigateway.config.ModelRoutingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ModelRoutingConfig config;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApiKeyAuthFilter(ModelRoutingConfig config,ObjectMapper objectMapper){
        this.config = config;
        this.objectMapper = objectMapper;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Skip auth for actuator and health endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.equals("/api/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Check if header exists
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Request rejected — missing X-API-Key header | path={}", path);
            sendError(response, 401, "Missing API key. Include X-API-Key header.");
            return;
        }

        // 2. Check if key is valid
        String clientId = findClientId(apiKey);
        if (clientId == null) {
            log.warn("Request rejected — invalid API key | path={}", path);
            sendError(response, 403, "Invalid API key.");
            return;
        }

        // 3. Attach clientId to request so controller can use it
        request.setAttribute("clientId", clientId);
        log.debug("Request authenticated | clientId={} path={}", clientId, path);

        // 4. Continue to controller
        filterChain.doFilter(request, response);
    }

    private String findClientId(String apiKey){
        return config.getApiKeys().entrySet().stream()
                .filter(entry ->entry.getValue().equals(apiKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private void sendError(HttpServletResponse response,int status,String message) throws IOException{
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(Map.of(
                        "status" ,status,
                        "error",status == 401 ?" UnAuthorized" : "Forbidden",
                            "message",message


                ))
        );
    }
}
