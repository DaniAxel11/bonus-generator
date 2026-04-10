package com.truper.bonusgenerator.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class AiClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${ai.api.url}")
    private String aiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    public String generarTexto(String prompt) {
        return webClientBuilder.build()
                .post()
                .uri(aiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue("{\"prompt\": \"" + prompt + "\"}")
                .retrieve()
                .bodyToMono(String.class)
                .block(); // bloqueante, se puede cambiar a reactive si quieres
    }
}