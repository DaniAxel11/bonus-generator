package com.truper.bonusgenerator.infrastructure.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truper.bonusgenerator.infrastructure.client.AiClient;
import com.truper.bonusgenerator.infrastructure.client.AiClient.AiAnalysisResponse;
import com.truper.bonusgenerator.model.dto.CommitDto;
import com.truper.bonusgenerator.model.dto.response.CommitMonthWeeksResponse;
import com.truper.bonusgenerator.service.commit.CommitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitAnalysisScheduler {

    private final CommitService commitService;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "${jobs.commit-analysis.cron}", zone = "${jobs.commit-analysis.zone}")
    public void analyzeLastCompleteWeekCommits() {
        log.info("Iniciando analisis automatico de commits de la ultima semana completa");

        CommitMonthWeeksResponse commitsByWeek = commitService.getCurrentMonthCommitsByWeek();
        String commitsJson = toJson(toAiPayload(commitsByWeek));
        AiAnalysisResponse response = aiClient.generarAnalisisCommitsConMetricas(commitsJson);

        log.info("Analisis automatico de commits generado: {}", response.analysis());
        log.info(
                "Metricas IA. promptTokens={}, responseTokens={}, totalTokens={}, responseTimeMs={}",
                response.usage().promptTokenCount(),
                response.usage().candidatesTokenCount(),
                response.usage().totalTokenCount(),
                response.usage().responseTimeMs()
        );
    }

    private AiCommitAnalysisPayload toAiPayload(CommitMonthWeeksResponse commitsByWeek) {
        List<AiCommitPayload> commits = commitsByWeek.getWeeks().stream()
                .flatMap(week -> week.getCommits().stream())
                .map(this::toAiCommitPayload)
                .toList();

        return new AiCommitAnalysisPayload(commits);
    }

    private AiCommitPayload toAiCommitPayload(CommitDto commit) {
        return new AiCommitPayload(commit.getRepo(), commit.getMessage());
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
