package com.actia.tracking_service.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaErrorConfig {

    private final KafkaProperties kafkaProperties;

    /**
     * Routes exhausted records to {@code {originalTopic}{dltSuffix}}.
     * The DLT suffix is read from {@link KafkaProperties.Topics#getDltSuffix()}.
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, String> kafkaTemplate) {

        String dltSuffix = kafkaProperties.getTopics().getDltSuffix();

        return new DeadLetterPublishingRecoverer(kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception ex) ->
                        new TopicPartition(record.topic() + dltSuffix, -1));
    }

    /**
     * Fixed-interval retry with DLT routing on exhaustion.
     * Configured via {@code tracking.kafka.retry.interval-ms} and
     * {@code tracking.kafka.retry.max-retries} in application.yaml.
     */
    @Bean
    public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer recoverer) {
        KafkaProperties.Retry retry = kafkaProperties.getRetry();
        FixedBackOff backOff = new FixedBackOff(retry.getIntervalMs(), retry.getMaxRetries());
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
