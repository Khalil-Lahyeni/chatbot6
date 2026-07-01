package com.actia.tracking_service.service;

import com.actia.tracking_service.dto.TrainSystemStatusDto;

import java.time.Instant;

/**
 * Orchestrates AI-state cache updates and publication to {@code train-ai-events}.
 *
 * Each method corresponds to one inbound Kafka topic.  The implementation
 * retrieves the current state from Redis, applies only the fields relevant to
 * that topic, refreshes {@code nbrCriticalMessage} from the sorted set, and
 * publishes to the AI topic if the state is complete.
 */
public interface TrainAiStateService {

    /** Called on every train-location event. No fields change; triggers publish if state is complete. */
    void onLocation(Long trainId);

    /** Called on every train-status event. Updates pacis/cctv/rearView/updateStatus. */
    void onSystemStatus(Long trainId, TrainSystemStatusDto dto);

    /**
     * Called on every train-message event.
     *
     * @param critical   whether the message was classified as critical
     * @param messageId  persisted entity ID, used as the sorted-set member
     * @param occurredAt creation timestamp of the message
     */
    void onMessage(Long trainId, Long messageId, boolean critical, Instant occurredAt);

    /** Called on every train-configuration event. Updates ccuVisible. */
    void onConfiguration(Long trainId, boolean visible);

    /**
     * Called on every train-media-database event.
     *
     * @param hasMultipleActive true when the train has more than one active MediaDatabase entry
     */
    void onMediaDatabase(Long trainId, boolean hasMultipleActive);
}
