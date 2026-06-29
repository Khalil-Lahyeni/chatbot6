package com.actia.tracking_service.service;

import com.actia.tracking_service.dto.TrainLocationDto;

/**
 * Port (DIP) — inbound processing contract for train location messages.
 *
 * Consumers depend on this interface; the implementation
 * ({@code TrainLocationProcessorImpl}) owns the dedup + persist + publish
 * orchestration.
 */
public interface TrainLocationProcessor {

    /**
     * Processes a validated, deserialized train location update.
     *
     * <p>Implementations are responsible for:
     * <ol>
     *   <li>Deduplication against the last known state</li>
     *   <li>Persistence to the database (when new)</li>
     *   <li>Publishing a downstream event (when new)</li>
     * </ol>
     *
     * @param dto the validated inbound location message
     */
    void process(TrainLocationDto dto);
}
