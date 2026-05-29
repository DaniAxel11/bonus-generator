package com.truper.bonusgenerator.infrastructure.scheduler;

import com.truper.bonusgenerator.model.dto.response.CommitAnalysisResponse;
import com.truper.bonusgenerator.service.analysis.CommitAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitAnalysisScheduler {

    private final CommitAnalysisService commitAnalysisService;

    @Scheduled(cron = "${jobs.commit-analysis.cron}", zone = "${jobs.commit-analysis.zone}")
    public void analyzeLastCompleteWeekCommits() {
        log.info("Iniciando analisis automatico de commits de la ultima semana completa");

        CommitAnalysisResponse response = commitAnalysisService.analyzeLastCompleteWeek();

        log.info("Analisis automatico de commits generado: {}", response.getAnalysis());
        log.info(
                "Metricas IA. promptTokens={}, responseTokens={}, totalTokens={}, responseTimeMs={}",
                response.getUsage().getPromptTokenCount(),
                response.getUsage().getCandidatesTokenCount(),
                response.getUsage().getTotalTokenCount(),
                response.getUsage().getResponseTimeMs()
        );

        log.info("Correo de analisis automatico de commits enviado");
    }
}
