package com.actia.tracking_service.strategy.impl;

import com.actia.tracking_service.dto.TrainSystemStatusDto;
import com.actia.tracking_service.strategy.HashContentExtractor;
import org.springframework.stereotype.Component;

/**
 * HashContentExtractor for inbound {@link TrainSystemStatusDto} messages.
 *
 * Enum names (not ordinals) are used so that inserting a new constant in the
 * middle of the enum declaration never silently changes existing hashes.
 */
@Component
public class TrainSystemStatusDtoHashExtractor implements HashContentExtractor<TrainSystemStatusDto> {

    @Override
    public String extract(TrainSystemStatusDto dto) {
        return dto.getPacisStatus().name()
                + FIELD_SEPARATOR
                + dto.getCctvStatus().name()
                + FIELD_SEPARATOR
                + dto.getRearViewStatus().name();
    }
}
