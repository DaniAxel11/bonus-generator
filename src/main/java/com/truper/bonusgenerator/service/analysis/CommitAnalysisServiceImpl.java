package com.truper.bonusgenerator.service.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truper.bonusgenerator.infrastructure.client.AiClient;
import com.truper.bonusgenerator.infrastructure.client.AiClient.AiAnalysisResponse;
import com.truper.bonusgenerator.model.dto.CommitDto;
import com.truper.bonusgenerator.model.dto.response.AiUsageResponse;
import com.truper.bonusgenerator.model.dto.response.CommitAnalysisResponse;
import com.truper.bonusgenerator.service.commit.CommitService;
import com.truper.bonusgenerator.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommitAnalysisServiceImpl implements CommitAnalysisService {

    private final CommitService commitService;
    private final AiClient aiClient;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Override
    public CommitAnalysisResponse analyzeLastCompleteWeek() {
        LocalDate startDate = getStartOfWeek(LocalDate.now()).minusWeeks(1);
        LocalDate endDate = startDate.plusDays(6);
        return analyzeByDateRange(startDate, endDate);
    }

    @Override
    public CommitAnalysisResponse analyzeByDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        List<CommitDto> commits = commitService.getCommitsByDateRange(startDate, endDate);
        if (commits.isEmpty()) {
            return new CommitAnalysisResponse(
                    startDate,
                    endDate,
                    0,
                    List.of("No se encontraron commits en el rango seleccionado.", "", ""),
                    new AiUsageResponse(0, 0, 0, 0),
                    false,
                    "No se envio correo porque no se encontraron commits en el rango seleccionado."
            );
        }

        String commitsJson = toJson(toAiPayload(commits));
        AiAnalysisResponse analysisResponse = aiClient.generarAnalisisCommitsConMetricas(commitsJson);
        emailService.sendCommitAnalysis(analysisResponse);

        return new CommitAnalysisResponse(
                startDate,
                endDate,
                commits.size(),
                analysisResponse.analysis(),
                toUsageResponse(analysisResponse),
                true,
                "Correo enviado correctamente."
        );
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate y endDate son obligatorios");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate no puede ser mayor que endDate");
        }
    }

    private LocalDate getStartOfWeek(LocalDate date) {
        int daysFromSunday = date.getDayOfWeek().getValue() % DayOfWeek.SUNDAY.getValue();
        return date.minusDays(daysFromSunday);
    }

    private AiUsageResponse toUsageResponse(AiAnalysisResponse response) {
        return new AiUsageResponse(
                response.usage().promptTokenCount(),
                response.usage().candidatesTokenCount(),
                response.usage().totalTokenCount(),
                response.usage().responseTimeMs()
        );
    }

    private AiCommitAnalysisPayload toAiPayload(List<CommitDto> commits) {
        return new AiCommitAnalysisPayload(
                commits.stream()
                        .map(commit -> new AiCommitPayload(commit.getRepo(), commit.getMessage()))
                        .toList()
        );
    }

    private String toJson(AiCommitAnalysisPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar los commits para la IA", exception);
        }
    }

    private record AiCommitAnalysisPayload(List<AiCommitPayload> commits) {
    }

    private record AiCommitPayload(String repo, String message) {
    }
}
