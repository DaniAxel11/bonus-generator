package com.truper.bonusgenerator.infrastructure.exception;

import com.truper.bonusgenerator.infrastructure.client.AiRateLimitException;
import com.truper.bonusgenerator.infrastructure.kafka.KafkaPublishException;
import com.truper.bonusgenerator.service.email.EmailSendException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AiRateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleAiRateLimit(AiRateLimitException exception) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", exception.getMessage());
        response.put("provider", "Gemini");
        response.put("model", exception.getModel());

        if (exception.getRetryAfter() != null) {
            response.put("retryAfter", exception.getRetryAfter());
        }

        if (exception.getProviderResponse() != null && !exception.getProviderResponse().isBlank()) {
            response.put("providerResponse", exception.getProviderResponse());
        }

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<Map<String, Object>> handleEmailSend(EmailSendException exception) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", exception.getMessage());
        response.put("provider", "SMTP");

        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            response.put("detail", exception.getCause().getMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(KafkaPublishException.class)
    public ResponseEntity<Map<String, Object>> handleKafkaPublish(KafkaPublishException exception) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", exception.getMessage());
        response.put("provider", "Kafka");

        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            response.put("detail", exception.getCause().getMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }
}
