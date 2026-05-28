package com.truper.bonusgenerator.infrastructure.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ai.api.url}")
    private String aiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.model}")
    private String model;

    public String generarTexto(String prompt) {
        return generarTextoConMetricas(prompt).text();
    }

    public AiTextResponse generarTextoConMetricas(String prompt) {
        Map<String, Object> request = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", prompt))
                        )
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        long startTime = System.nanoTime();
        JsonNode response = webClientBuilder.build()
                .post()
                .uri("%s/models/%s:generateContent".formatted(aiUrl, model))
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-goog-api-key", apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;

        AiUsage usage = extractUsage(response, responseTimeMs);
        String text = extractOutputText(response);

        log.info(
                "Respuesta IA recibida. modelo={}, promptTokens={}, responseTokens={}, totalTokens={}, responseTimeMs={}",
                model,
                usage.promptTokenCount(),
                usage.candidatesTokenCount(),
                usage.totalTokenCount(),
                usage.responseTimeMs()
        );

        return new AiTextResponse(text, usage);
    }

    public List<String> generarAnalisisCommits(String commitsJson) {
        return generarAnalisisCommitsConMetricas(commitsJson).analysis();
    }

    public AiAnalysisResponse generarAnalisisCommitsConMetricas(String commitsJson) {
        String prompt = """
                Analiza los siguientes commits.

                Regresa exclusivamente un JSON array con exactamente 3 strings:
                1. Indica el impacto positivo que lograste para la empresa, por que lo consideras relevante y cuales fueron las principales acciones que tu realizaste
                2. Describe el problema generado y cual crees que fue la causa raiz que lo origino
                3. Describe que acciones tomaste para eliminar la causa raiz y asegurar que no se vuelva a presentar el mismo problema

                No agregues markdown, explicaciones ni texto adicional.
                No mas de 500 caracteres por string.
                Commits:
                %s
                """.formatted(commitsJson);

        AiTextResponse response = generarTextoConMetricas(prompt);
        return new AiAnalysisResponse(parseStringArray(response.text()), response.usage());
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("La respuesta de la IA llego vacia");
        }

        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray()) {
            throw new IllegalStateException("La respuesta de Gemini no contiene el arreglo candidates");
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode candidate : candidates) {
            JsonNode parts = candidate.path("content").path("parts");
            if (!parts.isArray()) {
                continue;
            }
            for (JsonNode part : parts) {
                JsonNode textNode = part.path("text");
                if (!textNode.isMissingNode()) {
                    text.append(textNode.asText());
                }
            }
        }

        if (text.isEmpty()) {
            throw new IllegalStateException("La respuesta de la IA no contiene texto");
        }

        return text.toString();
    }

    private AiUsage extractUsage(JsonNode response, long responseTimeMs) {
        if (response == null) {
            return new AiUsage(0, 0, 0, responseTimeMs);
        }

        JsonNode usageMetadata = response.path("usageMetadata");
        return new AiUsage(
                usageMetadata.path("promptTokenCount").asInt(0),
                usageMetadata.path("candidatesTokenCount").asInt(0),
                usageMetadata.path("totalTokenCount").asInt(0),
                responseTimeMs
        );
    }

    private List<String> parseStringArray(String responseText) {
        try {
            List<String> response = objectMapper.readValue(responseText, new TypeReference<>() {
            });
            if (response.size() != 3) {
                throw new IllegalStateException("La IA debe regresar exactamente 3 strings");
            }
            return response;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("La respuesta de la IA no es un JSON array valido", exception);
        }
    }

    public record AiTextResponse(String text, AiUsage usage) {
    }

    public record AiAnalysisResponse(List<String> analysis, AiUsage usage) {
    }

    public record AiUsage(
            int promptTokenCount,
            int candidatesTokenCount,
            int totalTokenCount,
            long responseTimeMs
    ) {
    }
}
