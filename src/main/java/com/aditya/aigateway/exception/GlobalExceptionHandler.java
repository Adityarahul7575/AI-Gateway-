package com.aditya.aigateway.exception;

import com.aditya.aigateway.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles provider-specific errors (OpenAI, Claude, Gemini, Groq)
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientException(
            WebClientResponseException ex) {

        log.error("Provider API error | status={} message={}",
                ex.getStatusCode(), ex.getMessage());

        HttpStatus status = switch (ex.getStatusCode().value()) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            case 404 -> HttpStatus.NOT_FOUND;
            case 500 -> HttpStatus.BAD_GATEWAY;
            default  -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        String message = switch (ex.getStatusCode().value()) {
            case 401 -> "Provider authentication failed. Check API key.";
            case 429 -> "Provider rate limit exceeded. Please retry later.";
            case 404 -> "Model not found. Check model name in configuration.";
            case 500 -> "Provider internal error. Try again later.";
            default  -> "Provider returned an unexpected error.";
        };

        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .requestId("req-" + UUID.randomUUID().toString().substring(0, 8))
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // Handles unknown model/provider errors
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.error("Invalid request | message={}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(400)
                        .error("Bad Request")
                        .message(ex.getMessage())
                        .requestId("req-" + UUID.randomUUID().toString().substring(0, 8))
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // Handles our custom provider exceptions
    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<ErrorResponse> handleProviderException(
            ProviderException ex) {

        log.error("Provider exception | provider={} message={}",
                ex.getProvider(), ex.getMessage());

        return ResponseEntity.status(ex.getStatus()).body(
                ErrorResponse.builder()
                        .status(ex.getStatus().value())
                        .error(ex.getStatus().getReasonPhrase())
                        .message(ex.getMessage())
                        .requestId("req-" + UUID.randomUUID().toString().substring(0, 8))
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // Handles our custom gateway exceptions
    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ErrorResponse> handleGatewayException(
            GatewayException ex) {

        log.error("Gateway exception | message={}", ex.getMessage());

        return ResponseEntity.status(ex.getStatus()).body(
                ErrorResponse.builder()
                        .status(ex.getStatus().value())
                        .error(ex.getStatus().getReasonPhrase())
                        .message(ex.getMessage())
                        .requestId("req-" + UUID.randomUUID().toString().substring(0, 8))
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // Catches everything else
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {

        log.error("Unexpected error | message={}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .status(500)
                        .error("Internal Server Error")
                        .message("An unexpected error occurred.")
                        .requestId("req-" + UUID.randomUUID().toString().substring(0, 8))
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}