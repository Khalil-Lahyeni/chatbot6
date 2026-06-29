package com.actia.tracking_service.service;

import com.actia.tracking_service.dto.TrainSystemStatusDto;

/**
 * Port (DIP) — inbound processing contract for train system-status messages.
 *
 * Mirrors {@link TrainLocationProcessor}; kept separate per ISP so consumers
 * only depend on the interface they actually use.
 */
public interface TrainSystemStatusProcessor {

    /**
     * Processes a validated, deserialized train system-status update.
     *
     * <p>Implementations are responsible for:
     * <ol>
     *   <li>Deduplication against the last known state</li>
     *   <li>Persistence to the database (when new)</li>
     *   <li>Publishing a downstream event (when new)</li>
     * </ol>
     *
     * @param dto the validated inbound system-status message
     */
    void process(TrainSystemStatusDto dto);
}
