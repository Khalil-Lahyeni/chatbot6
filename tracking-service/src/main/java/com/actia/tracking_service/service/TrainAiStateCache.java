package com.actia.tracking_service.service;

import com.actia.tracking_service.dto.TrainAiStateDto;

import java.time.Instant;
import java.util.Optional;

/**
 * Port for the AI-state Redis cache.
 *
 * Each train has two Redis structures:
 * <ul>
 *   <li>{@code ai:train:{trainId}} — JSON snapshot of {@link TrainAiStateDto}.</li>
 *   <li>{@code ai:train:{trainId}:critical} — Sorted Set of critical message IDs
 *       scored by epoch-seconds, used for accurate 1-hour sliding-window counts.</li>
 * </ul>
 */
public interface TrainAiStateCache {

    /** Returns the current AI state for the train, or empty if not yet seeded. */
    Optional<TrainAiStateDto> get(Long trainId);

    /** Persists the AI state for the train with no TTL. */
    void save(Long trainId, TrainAiStateDto state);

    /**
     * Registers a critical message in the sorted set and returns the accurate
     * count of critical messages within the last 60 minutes.
     *
     * @param trainId   the train to update
     * @param messageId unique identifier used as the sorted-set member
     * @param occurredAt timestamp of the message (score in the sorted set)
     */
    long addCriticalAndCount(Long trainId, Long messageId, Instant occurredAt);

    /**
     * Returns the count of critical messages within the last 60 minutes
     * without adding a new entry.  Old entries are pruned before counting.
     */
    long countCriticalLastHour(Long trainId);

    /**
     * Seeds the critical sorted set from warmup data.
     *
     * @param trainId    the train to seed
     * @param messageId  sorted-set member
     * @param occurredAt score (creation timestamp)
     */
    void seedCritical(Long trainId, Long messageId, Instant occurredAt);
}
