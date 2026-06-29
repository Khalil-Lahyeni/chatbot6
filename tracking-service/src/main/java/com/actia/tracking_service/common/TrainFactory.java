package com.actia.tracking_service.common;

import com.actia.tracking_service.entity.Train;

/**
 * Factory Pattern — centralises the creation of Train entities.
 *
 * Avoids inline construction logic scattered across multiple service classes
 * and ensures placeholder trains are always built consistently.
 */
public final class TrainFactory {

    /** Utility class — no instantiation. */
    private TrainFactory() {}

    /**
     * Creates a minimal placeholder Train for a train ID that is not yet
     * registered in the database.
     *
     * <p>The placeholder name follows the convention {@code UNKNOWN-{id}} so
     * it is immediately identifiable in queries and logs.
     *
     * @param trainId the ID received in the incoming Kafka message
     * @return an unsaved Train entity ready to be persisted
     */
    public static Train createPlaceholder(Long trainId) {
        return Train.builder()
                .trainId(trainId)
                .name("UNKNOWN-" + trainId)
                .build();
    }
}
