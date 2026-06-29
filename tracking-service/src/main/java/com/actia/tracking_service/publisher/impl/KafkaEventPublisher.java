package com.actia.tracking_service.publisher.impl;

import com.actia.tracking_service.config.KafkaProperties;
import com.actia.tracking_service.exception.EventPublishingException;
import com.actia.tracking_service.publisher.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed implementation of {@link EventPublisher}.
 *
 * The events topic name is read from {@link KafkaProperties.Topics#getEvents()}
 * so it can be changed in application.yaml without touching this class.
 *
 * The message key equals {@code entityId} (trainId) so all events for the
 * same train land on the same partition and are consumed in order.
 *
 * Publication is best-effort: failures are wrapped in
 * {@link EventPublishingException} for structured logging and then swallowed.
 * For at-least-once delivery, adopt the Outbox pattern.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper                  objectMapper;
    private final KafkaProperties               kafkaProperties;

    @Override
    public void publish(String entityId, Object payload) {
        String topic = kafkaProperties.getTopics().getEvents();
        String json;

        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            EventPublishingException pubEx = new EventPublishingException(entityId, topic, ex);
            log.error("Serialisation failed — event dropped: {}", pubEx.getMessage());
            return;
        }

        kafkaTemplate.send(topic, entityId, json)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        EventPublishingException pubEx = new EventPublishingException(entityId, topic, ex);
                        log.error("Async send failed — event dropped (consider Outbox pattern): {}",
                                pubEx.getMessage());
                    } else {
                        log.debug("Event published — entityId={} offset={}",
                                entityId, result.getRecordMetadata().offset());
                    }
                });
    }
}
