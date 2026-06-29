package com.actia.tracking_service.strategy.impl;

import com.actia.tracking_service.entity.TrainSystemStatus;
import com.actia.tracking_service.strategy.HashContentExtractor;
import org.springframework.stereotype.Component;

/**
 * HashContentExtractor for persisted {@link TrainSystemStatus} entities.
 *
 * Used by {@code SystemStatusCacheWarmupContributor} to rebuild the Redis dedup
 * cache at startup.  Field order and separator are identical to
 * {@link TrainSystemStatusDtoHashExtractor}.
 */
@Component
public class TrainSystemStatusEntityHashExtractor implements HashContentExtractor<TrainSystemStatus> {

    @Override
    public String extract(TrainSystemStatus entity) {
        return entity.getPacisStatus().name()
                + FIELD_SEPARATOR
                + entity.getCctvStatus().name()
                + FIELD_SEPARATOR
                + entity.getRearViewStatus().name();
    }
}
