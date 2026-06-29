package com.actia.tracking_service.exception;

/**
 * Thrown when a train ID referenced in an incoming message is not found in
 * the registry (database) and auto-registration is disabled.
 *
 * @see com.actia.tracking_service.config.TrainProperties#isAutoRegisterUnknown()
 * @see com.actia.tracking_service.common.TrainResolver
 */
public class TrainNotFoundException extends TrackingServiceException {

    private final Long trainId;

    public TrainNotFoundException(Long trainId) {
        super("Train not found in registry: " + trainId
                + " — set tracking.train.auto-register-unknown=true to create a placeholder");
        this.trainId = trainId;
    }

    public Long getTrainId() {
        return trainId;
    }
}
