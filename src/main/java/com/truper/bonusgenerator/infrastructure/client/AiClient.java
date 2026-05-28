package com.truper.bonusgenerator.infrastructure.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
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
        Map<String, Object> request = Map.of(
                "model", model,
                "input", prompt
        );

        JsonNode response = webClientBuilder.build()
                .post()
                .uri(aiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return extractOutputText(response);
    }

    public List<String> generarAnalisisCommits(String commitsJson) {
        String prompt = """
                Analiza los siguientes commits.

                Regresa exclusivamente un JSON array con exactamente 3 strings:
                1. Indica el impacto positivo que lograste para la empresa, por qué lo consideras relevante y cuáles fueron las principales acciones que tu realizaste
                2. Describe el problema generado y cuál crees que fue la causa raíz que lo originó
                3. Describe que acciones tomaste para eliminar la causa raíz y asegurar que no se vuelva a presentar el mismo problema

                No agregues markdown, explicaciones ni texto adicional.

                Commits:
                %s
                """.formatted(commitsJson);

        String responseText = generarTexto(prompt);
        return parseStringArray(responseText);
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("La respuesta de la IA llego vacia");
        }

        JsonNode output = response.path("output");
        if (!output.isArray()) {
            throw new IllegalStateException("La respuesta de la IA no contiene el arreglo output");
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode outputItem : output) {
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                JsonNode textNode = contentItem.path("text");
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
}
