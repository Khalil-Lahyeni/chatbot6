package com.actia.tracking_service.consumer;

import com.actia.tracking_service.dto.TrainSystemStatusDto;
import com.actia.tracking_service.service.TrainSystemStatusProcessor;
import com.actia.tracking_service.validation.MessageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the train-status topic.
 *
 * Topic name and consumer group are resolved at startup from
 * {@code tracking.kafka.topics.system-status} and
 * {@code tracking.kafka.consumer.group-id} in application.yaml.
 *
 * DIP: depends on {@link TrainSystemStatusProcessor} (interface).
 */
@Component
public class TrainSystemStatusConsumer extends AbstractMessageConsumer<TrainSystemStatusDto> {

    private final TrainSystemStatusProcessor processor;

    public TrainSystemStatusConsumer(ObjectMapper objectMapper,
                                     MessageValidator validator,
                                     TrainSystemStatusProcessor processor) {
        super(objectMapper, validator, TrainSystemStatusDto.class);
        this.processor = processor;
    }

    @KafkaListener(
            topics           = "${tracking.kafka.topics.system-status}",
            groupId          = "${tracking.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message) {
        handleMessage(message);
    }

    @Override
    protected void process(TrainSystemStatusDto dto) {
        processor.process(dto);
    }
}
