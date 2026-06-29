package com.actia.tracking_service.consumer;

import com.actia.tracking_service.dto.TrainMessageDto;
import com.actia.tracking_service.service.TrainMessageProcessor;
import com.actia.tracking_service.validation.MessageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the train-message topic.
 *
 * Topic name is resolved at startup from
 * {@code tracking.kafka.topics.train-message} in application.yaml.
 *
 * Unlike the location and system-status consumers, this consumer does NOT
 * perform deduplication — every validated message is forwarded to
 * {@link TrainMessageProcessor} for persistence.
 */
@Component
public class TrainMessageConsumer extends AbstractMessageConsumer<TrainMessageDto> {

    private final TrainMessageProcessor processor;

    public TrainMessageConsumer(ObjectMapper objectMapper,
                                MessageValidator validator,
                                TrainMessageProcessor processor) {
        super(objectMapper, validator, TrainMessageDto.class);
        this.processor = processor;
    }

    @KafkaListener(
            topics           = "${tracking.kafka.topics.train-message}",
            groupId          = "${tracking.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message) {
        handleMessage(message);
    }

    @Override
    protected void process(TrainMessageDto dto) {
        processor.process(dto);
    }
}
