package com.truper.bonusgenerator.infrastructure.client;

import lombok.Getter;

@Getter
public class AiRateLimitException extends RuntimeException {

    private final String retryAfter;
    private final String providerResponse;
    private final String model;

    public AiRateLimitException(String message, String retryAfter, String providerResponse, String model, Throwable cause) {
        super(message, cause);
        this.retryAfter = retryAfter;
        this.providerResponse = providerResponse;
        this.model = model;
    }
}
