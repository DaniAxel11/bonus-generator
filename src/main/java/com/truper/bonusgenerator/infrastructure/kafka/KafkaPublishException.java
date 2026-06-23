package com.truper.bonusgenerator.infrastructure.kafka;

public class KafkaPublishException extends RuntimeException {

    public KafkaPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
