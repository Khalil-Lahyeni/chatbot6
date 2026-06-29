package com.actia.tracking_service.dto;

import java.time.Instant;

/**
 * Outbound representation of a {@link com.actia.tracking_service.entity.Train}.
 * Immutable record — safe for serialization and caching.
 */
public record TrainResponse(
        Long    trainId,
        String  name,
        Instant updateAt,
        String  mission,
        String  baseline,
        String  diversity,
        String  database
) {}
