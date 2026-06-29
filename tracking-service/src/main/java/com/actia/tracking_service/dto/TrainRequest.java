package com.actia.tracking_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Inbound payload for POST /trains (create) and PUT /trains/{id} (update).
 *
 * trainId is required on create because the train identifier is assigned
 * externally (no auto-increment in the DB schema).
 */
public record TrainRequest(

        @NotNull(message = "trainId is required")
        @Positive(message = "trainId must be a positive number")
        Long trainId,

        String name,
        String mission,
        String baseline,
        String diversity,
        String database
) {}
