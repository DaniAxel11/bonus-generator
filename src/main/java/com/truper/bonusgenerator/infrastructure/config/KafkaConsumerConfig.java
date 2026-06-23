package com.truper.bonusgenerator.infrastructure.config;

import com.truper.bonusgenerator.infrastructure.kafka.event.CommitAnalysisRequestedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Bean
    public ConsumerFactory<String, CommitAnalysisRequestedEvent> commitAnalysisConsumerFactory() {
        JsonDeserializer<CommitAnalysisRequestedEvent> valueDeserializer = new JsonDeserializer<>(
                CommitAnalysisRequestedEvent.class
        );
        valueDeserializer.addTrustedPackages("com.truper.bonusgenerator.infrastructure.kafka.event");
        valueDeserializer.setUseTypeHeaders(false);

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, CommitAnalysisRequestedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, CommitAnalysisRequestedEvent> commitAnalysisConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, CommitAnalysisRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(commitAnalysisConsumerFactory);
        return factory;
    }
}
