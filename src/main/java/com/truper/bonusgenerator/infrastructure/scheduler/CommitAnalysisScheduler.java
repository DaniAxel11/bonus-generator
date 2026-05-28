package com.truper.bonusgenerator.infrastructure.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truper.bonusgenerator.infrastructure.client.AiClient;
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
        String commitsJson = toJson(commitsByWeek);
        List<String> analysis = aiClient.generarAnalisisCommits(commitsJson);

        log.info("Analisis automatico de commits generado: {}", analysis);
    }

    private String toJson(CommitMonthWeeksResponse commitsByWeek) {
        try {
            return objectMapper.writeValueAsString(commitsByWeek);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar los commits para la IA", exception);
        }
    }
}
