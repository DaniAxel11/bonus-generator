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
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

    @Value("${ai.api.fallback-model:}")
    private String fallbackModel;

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
        AiRawResponse rawResponse;
        try {
            rawResponse = sendRequest(model, request, startTime);
        } catch (WebClientResponseException.TooManyRequests exception) {
            rawResponse = retryWithFallbackModel(request, startTime, exception);
        } catch (WebClientResponseException exception) {
            throw new IllegalStateException(
                    "Gemini rechazo la solicitud. status=%s, body=%s".formatted(
                            exception.getStatusCode(),
                            exception.getResponseBodyAsString()
                    ),
                    exception
            );
        }

        AiUsage usage = extractUsage(rawResponse.response(), rawResponse.responseTimeMs());
        String text = extractOutputText(rawResponse.response());

        log.info(
                "Respuesta IA recibida. modelo={}, promptTokens={}, responseTokens={}, totalTokens={}, responseTimeMs={}",
                rawResponse.model(),
                usage.promptTokenCount(),
                usage.candidatesTokenCount(),
                usage.totalTokenCount(),
                usage.responseTimeMs()
        );

        return new AiTextResponse(text, usage);
    }

    private AiRawResponse sendRequest(String requestModel, Map<String, Object> request, long startTime) {
        String responseBody = webClientBuilder.build()
                .post()
                .uri("%s/models/%s:generateContent".formatted(aiUrl, requestModel))
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-goog-api-key", apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;
        return new AiRawResponse(requestModel, parseResponseBody(responseBody), responseTimeMs);
    }

    private AiRawResponse retryWithFallbackModel(
            Map<String, Object> request,
            long startTime,
            WebClientResponseException.TooManyRequests originalException
    ) {
        if (fallbackModel == null || fallbackModel.isBlank() || fallbackModel.equals(model)) {
            throw toRateLimitException(originalException, model);
        }

        log.warn(
                "Gemini regreso 429 para modelo {}. Reintentando con fallbackModel={}",
                model,
                fallbackModel
        );

        try {
            return sendRequest(fallbackModel, request, startTime);
        } catch (WebClientResponseException.TooManyRequests fallbackException) {
            throw toRateLimitException(fallbackException, fallbackModel);
        }
    }

    private AiRateLimitException toRateLimitException(
            WebClientResponseException.TooManyRequests exception,
            String requestModel
    ) {
        return new AiRateLimitException(
                "Gemini rechazo la solicitud por limite de cuota o frecuencia. Espera unos minutos o revisa los limites del proyecto en Google AI Studio.",
                exception.getHeaders().getFirst("Retry-After"),
                exception.getResponseBodyAsString(),
                requestModel,
                exception
        );
    }

    public List<String> generarAnalisisCommits(String commitsJson) {
        return generarAnalisisCommitsConMetricas(commitsJson).analysis();
    }

    public AiAnalysisResponse generarAnalisisCommitsConMetricas(String commitsJson) {
        String prompt = """
                Analiza los siguientes commits con un enfoque de desarrollador Java y debe ser informal sin tantos tecnisismos.

                No agregues markdown, explicaciones ni texto adicional.
                No mas de 500 caracteres por string.
                Actúa como un desarrollador de software documentando actividades realizadas durante un periodo de trabajo.
                
                Genera tres comentarios breves (máximo 500 caracteres cada uno) con un tono profesional pero natural, evitando lenguaje excesivamente formal o corporativo.
                Regresa exclusivamente un JSON array con exactamente 3 strings.

               A partir de los commits genera:
                
                1. Impacto generado
                   * Describe las acciones realizadas.
                   * Explica el beneficio o impacto positivo para la empresa o el sistema.
                   * Enfócate en las contribuciones realizadas por el desarrollador.
                2. Problema y causa raíz
                   * Describe el problema presentado.
                   * Explica cuál fue la causa raíz identificada.
                   * Mantén un enfoque objetivo y técnico, sin señalar responsables.
                3. Solución implementada 
                   * Describe las acciones realizadas para corregir el problema.
                   * Explica cómo se previene que vuelva a ocurrir.
                   * Menciona validaciones, pruebas o mejoras implementadas.
                
                Estilo de redacción:
                
                * Usar verbos en primera persona: "Realicé", "Actualicé", "Corregí", "Validé", "Implementé".
                * Redactar en un solo párrafo por comentario.
                * Ser concreto y específico.
                * Resaltar estabilidad, seguridad, mantenimiento, automatización o continuidad operativa cuando aplique.
                * Evitar listas, viñetas y tecnicismos innecesarios.
                * Mantener una extensión aproximada de 300 a 500 caracteres por comentario.
                
                Información de entrada, commits:
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

    private JsonNode parseResponseBody(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("La respuesta de Gemini no es JSON valido", exception);
        }
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

    private record AiRawResponse(String model, JsonNode response, long responseTimeMs) {
    }
}
