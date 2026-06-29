package com.actia.tracking_service.exception;

/**
 * Signals a failure to publish a domain event to the Kafka events topic.
 *
 * Event publication in this service is <em>best-effort</em>: the exception is
 * created for structured logging and caught internally.  For guaranteed
 * delivery, adopt the Outbox pattern and let a separate relay process handle
 * republication.
 */
public class EventPublishingException extends TrackingServiceException {

    private final String entityId;
    private final String topic;

    public EventPublishingException(String entityId, String topic, Throwable cause) {
        super("Failed to publish event for entityId='" + entityId
                + "' to topic='" + topic + "': " + cause.getMessage(), cause);
        this.entityId = entityId;
        this.topic    = topic;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getTopic() {
        return topic;
    }
}
