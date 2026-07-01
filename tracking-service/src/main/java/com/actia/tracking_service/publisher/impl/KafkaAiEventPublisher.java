package com.actia.tracking_service.publisher.impl;

import com.actia.tracking_service.config.KafkaProperties;
import com.actia.tracking_service.dto.TrainAiStateDto;
import com.actia.tracking_service.exception.EventPublishingException;
import com.actia.tracking_service.publisher.AiEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed implementation of {@link AiEventPublisher}.
 *
 * Publishes to the {@code train-ai-events} topic (resolved from
 * {@link KafkaProperties.Topics#getAiEvents()}).  The train ID is used as
 * the message key to guarantee per-train ordering.
 *
 * Failures are logged and swallowed — same best-effort contract as
 * {@link KafkaEventPublisher}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAiEventPublisher implements AiEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper                  objectMapper;
    private final KafkaProperties               kafkaProperties;

    @Override
    public void publish(String trainId, TrainAiStateDto state) {
        String topic = kafkaProperties.getTopics().getAiEvents();
        String json;

        try {
            json = objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException ex) {
            EventPublishingException pubEx = new EventPublishingException(trainId, topic, ex);
            log.error("AI state serialisation failed — event dropped: {}", pubEx.getMessage());
            return;
        }

        kafkaTemplate.send(topic, trainId, json)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        EventPublishingException pubEx = new EventPublishingException(trainId, topic, ex);
                        log.error("AI state async send failed — event dropped: {}", pubEx.getMessage());
                    } else {
                        log.debug("AI state published — trainId={} offset={}",
                                trainId, result.getRecordMetadata().offset());
                    }
                });
    }
}
