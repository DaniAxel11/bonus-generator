package com.truper.bonusgenerator.infrastructure.kafka.consumer;

import com.truper.bonusgenerator.infrastructure.kafka.event.CommitAnalysisRequestedEvent;
import com.truper.bonusgenerator.service.analysis.CommitAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitAnalysisEventConsumer {

    private final CommitAnalysisService commitAnalysisService;

    @KafkaListener(topics = "${app.kafka.topics.commit-analysis-requested}")
    public void consumeCommitAnalysisRequested(CommitAnalysisRequestedEvent event) {
        log.info(
                "Solicitud de analisis de commits recibida desde Kafka. startDate={}, endDate={}, source={}",
                event.startDate(),
                event.endDate(),
                event.source()
        );

        commitAnalysisService.analyzeByDateRange(event.startDate(), event.endDate());

        log.info(
                "Solicitud de analisis de commits procesada desde Kafka. startDate={}, endDate={}",
                event.startDate(),
                event.endDate()
        );
    }
}
