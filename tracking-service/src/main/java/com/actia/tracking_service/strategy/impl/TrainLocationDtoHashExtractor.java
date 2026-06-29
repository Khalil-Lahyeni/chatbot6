package com.actia.tracking_service.strategy.impl;

import com.actia.tracking_service.dto.TrainLocationDto;
import com.actia.tracking_service.strategy.HashContentExtractor;
import org.springframework.stereotype.Component;

/**
 * HashContentExtractor for inbound {@link TrainLocationDto} messages.
 *
 * Used by {@code TrainLocationProcessorImpl} to determine whether an incoming
 * Kafka message carries new data.
 */
@Component
public class TrainLocationDtoHashExtractor implements HashContentExtractor<TrainLocationDto> {

    @Override
    public String extract(TrainLocationDto dto) {
        return dto.getCurrentStation()
                + FIELD_SEPARATOR
                + dto.getNextStation()
                + FIELD_SEPARATOR
                + dto.getDestination();
    }
}
