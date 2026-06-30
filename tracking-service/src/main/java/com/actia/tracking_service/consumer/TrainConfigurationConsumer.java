package com.actia.tracking_service.consumer;

import com.actia.tracking_service.dto.TrainConfigurationDto;
import com.actia.tracking_service.service.TrainConfigurationProcessor;
import com.actia.tracking_service.validation.MessageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the train-configuration topic.
 *
 * Topic name is resolved at startup from
 * {@code tracking.kafka.topics.configuration} in application.yaml.
 *
 * No deduplication — every validated message is forwarded to
 * {@link TrainConfigurationProcessor} for persistence and event publishing.
 */
@Component
public class TrainConfigurationConsumer extends AbstractMessageConsumer<TrainConfigurationDto> {

    private final TrainConfigurationProcessor processor;

    public TrainConfigurationConsumer(ObjectMapper objectMapper,
                                      MessageValidator validator,
                                      TrainConfigurationProcessor processor) {
        super(objectMapper, validator, TrainConfigurationDto.class);
        this.processor = processor;
    }

    @KafkaListener(
            topics           = "${tracking.kafka.topics.configuration}",
            groupId          = "${tracking.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message) {
        handleMessage(message);
    }

    @Override
    protected void process(TrainConfigurationDto dto) {
        processor.process(dto);
    }
}
