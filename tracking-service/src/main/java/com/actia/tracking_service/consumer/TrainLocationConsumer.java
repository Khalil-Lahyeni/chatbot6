package com.actia.tracking_service.consumer;

import com.actia.tracking_service.dto.TrainLocationDto;
import com.actia.tracking_service.service.TrainLocationProcessor;
import com.actia.tracking_service.validation.MessageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the train-location topic.
 *
 * Topic name and consumer group are resolved at startup from
 * {@code tracking.kafka.topics.location} and
 * {@code tracking.kafka.consumer.group-id} in application.yaml — no source
 * code change is required to rename a topic or reassign the consumer group.
 *
 * DIP: depends on {@link TrainLocationProcessor} (interface).
 */
@Component
public class TrainLocationConsumer extends AbstractMessageConsumer<TrainLocationDto> {

    private final TrainLocationProcessor processor;

    public TrainLocationConsumer(ObjectMapper objectMapper,
                                 MessageValidator validator,
                                 TrainLocationProcessor processor) {
        super(objectMapper, validator, TrainLocationDto.class);
        this.processor = processor;
    }

    @KafkaListener(
            topics           = "${tracking.kafka.topics.location}",
            groupId          = "${tracking.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message) {
        handleMessage(message);
    }

    @Override
    protected void process(TrainLocationDto dto) {
        processor.process(dto);
    }
}
