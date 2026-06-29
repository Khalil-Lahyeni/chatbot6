package com.actia.tracking_service.strategy.impl;

import com.actia.tracking_service.entity.TrainLocation;
import com.actia.tracking_service.strategy.HashContentExtractor;
import org.springframework.stereotype.Component;

/**
 * HashContentExtractor for persisted {@link TrainLocation} entities.
 *
 * Used by {@code LocationCacheWarmupContributor} to rebuild the Redis dedup
 * cache at startup from the latest database state.
 *
 * Field order and separator are intentionally identical to
 * {@link TrainLocationDtoHashExtractor} so that a message with the same
 * station values produces the exact same hash regardless of the source type.
 */
@Component
public class TrainLocationEntityHashExtractor implements HashContentExtractor<TrainLocation> {

    @Override
    public String extract(TrainLocation entity) {
        return entity.getCurrentStation()
                + FIELD_SEPARATOR
                + entity.getNextStation()
                + FIELD_SEPARATOR
                + entity.getDestination();
    }
}
