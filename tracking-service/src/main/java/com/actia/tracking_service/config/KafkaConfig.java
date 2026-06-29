package com.actia.tracking_service.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    // ── Topics ────────────────────────────────────────────────────────────────

    @Bean
    public NewTopic trainEventsTopic() {
        KafkaProperties.Topics t = kafkaProperties.getTopics();
        return TopicBuilder.name(t.getEvents())
                .partitions(t.getEventsPartitions())
                .replicas(t.getEventsReplicas())
                .build();
    }

    @Bean
    public NewTopic trainLocationDlt() {
        KafkaProperties.Topics t = kafkaProperties.getTopics();
        return TopicBuilder.name(t.getLocation() + t.getDltSuffix())
                .partitions(t.getDltPartitions())
                .replicas(t.getDltReplicas())
                .build();
    }

    @Bean
    public NewTopic trainStatusDlt() {
        KafkaProperties.Topics t = kafkaProperties.getTopics();
        return TopicBuilder.name(t.getSystemStatus() + t.getDltSuffix())
                .partitions(t.getDltPartitions())
                .replicas(t.getDltReplicas())
                .build();
    }

    @Bean
    public NewTopic trainMessageDlt() {
        KafkaProperties.Topics t = kafkaProperties.getTopics();
        return TopicBuilder.name(t.getTrainMessage() + t.getDltSuffix())
                .partitions(t.getDltPartitions())
                .replicas(t.getDltReplicas())
                .build();
    }

    // ── Listener container factory ────────────────────────────────────────────

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(kafkaProperties.getConsumer().getConcurrency());
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
