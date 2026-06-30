package com.actia.tracking_service.consumer;

import com.actia.tracking_service.dto.MediaDatabaseDto;
import com.actia.tracking_service.service.MediaDatabaseProcessor;
import com.actia.tracking_service.validation.MessageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the train-media-database topic.
 *
 * Topic name is resolved at startup from
 * {@code tracking.kafka.topics.media-database} in application.yaml.
 *
 * No deduplication — every validated message is forwarded to
 * {@link MediaDatabaseProcessor} for persistence and event publishing.
 */
@Component
public class MediaDatabaseConsumer extends AbstractMessageConsumer<MediaDatabaseDto> {

    private final MediaDatabaseProcessor processor;

    public MediaDatabaseConsumer(ObjectMapper objectMapper,
                                 MessageValidator validator,
                                 MediaDatabaseProcessor processor) {
        super(objectMapper, validator, MediaDatabaseDto.class);
        this.processor = processor;
    }

    @KafkaListener(
            topics           = "${tracking.kafka.topics.media-database}",
            groupId          = "${tracking.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message) {
        handleMessage(message);
    }

    @Override
    protected void process(MediaDatabaseDto dto) {
        processor.process(dto);
    }
}
