package com.aditya.aigateway.controller;


import com.aditya.aigateway.exception.GatewayException;
import com.aditya.aigateway.model.GatewayRequest;
import com.aditya.aigateway.model.GatewayResponse;
import com.aditya.aigateway.ratelimit.RateLimiterService;
import com.aditya.aigateway.service.GatewayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GatewayController {
    private final GatewayService gatewayService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/chat")
    public ResponseEntity<GatewayResponse> chat(@RequestBody GatewayRequest request, HttpServletRequest httpRequest) {

        //client id :
        String clientId = (String) httpRequest.getAttribute("clientId");
        request.setClientId(clientId);

        // Check rate limit before processing
        if (!rateLimiterService.isAllowed(clientId)) {
            throw new GatewayException(
                    "Rate limit exceeded. Max " +
                            "requests per minute allowed. Try again later.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }


        log.info("Received request | client_id = {} tier={} task={}", request.getClientId(),request.getTier(), request.getTask());
        GatewayResponse response = gatewayService.process(request);
        return ResponseEntity.ok(response);
    }

    // Health check for the gateway itself
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Gateway is running");
    }

}
