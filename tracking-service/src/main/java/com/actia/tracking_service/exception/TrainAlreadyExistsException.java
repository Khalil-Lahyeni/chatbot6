package com.actia.tracking_service.exception;

/**
 * Thrown when a POST /trains request supplies a trainId that already exists
 * in the database.  Maps to HTTP 409 Conflict.
 */
public class TrainAlreadyExistsException extends TrackingServiceException {

    private final Long trainId;

    public TrainAlreadyExistsException(Long trainId) {
        super("Train already exists with id: " + trainId);
        this.trainId = trainId;
    }

    public Long getTrainId() {
        return trainId;
    }
}
