package com.actia.tracking_service.strategy;

/**
 * Strategy — determines whether a message name represents a critical event.
 *
 * Decouples the detection logic from the processor so the algorithm
 * (keyword list, regex, ML score, etc.) can be swapped without touching
 * {@link com.actia.tracking_service.service.impl.TrainMessageProcessorImpl}.
 */
@FunctionalInterface
public interface CriticalEventDetector {

    /**
     * @param messageName the {@code messageName} field from the incoming DTO
     * @return {@code true} if the message should be flagged as critical
     */
    boolean isCritical(String messageName);
}
