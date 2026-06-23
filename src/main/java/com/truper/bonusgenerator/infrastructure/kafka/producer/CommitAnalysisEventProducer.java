package com.truper.bonusgenerator.infrastructure.kafka.producer;

import com.truper.bonusgenerator.infrastructure.kafka.KafkaPublishException;
import com.truper.bonusgenerator.infrastructure.kafka.event.CommitAnalysisRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitAnalysisEventProducer {

    private final KafkaTemplate<String, CommitAnalysisRequestedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.commit-analysis-requested}")
    private String commitAnalysisRequestedTopic;

    public void publishCommitAnalysisRequested(CommitAnalysisRequestedEvent event) {
        String key = event.startDate() + ":" + event.endDate();

        try {
            SendResult<String, CommitAnalysisRequestedEvent> result = kafkaTemplate
                    .send(commitAnalysisRequestedTopic, key, event)
                    .get(10, TimeUnit.SECONDS);

            log.info(
                    "Solicitud de analisis de commits publicada en Kafka. topic={}, partition={}, offset={}, key={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    key
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaPublishException("Publicacion en Kafka interrumpida", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaPublishException("No fue posible publicar la solicitud de analisis en Kafka", exception);
        }
    }
}
