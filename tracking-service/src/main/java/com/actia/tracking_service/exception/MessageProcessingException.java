package com.actia.tracking_service.exception;

/**
 * Wraps unexpected failures that occur during message processing (database
 * errors, mapping failures, etc.) after a message has already been validated.
 *
 * Unlike {@link MessageValidationException}, this exception IS retryable —
 * transient infrastructure errors may resolve on the next attempt.
 * Spring Kafka's {@link org.springframework.kafka.listener.DefaultErrorHandler}
 * will retry and eventually route to the Dead Letter Topic.
 */
public class MessageProcessingException extends TrackingServiceException {

    private final String messageType;

    public MessageProcessingException(String messageType, Throwable cause) {
        super("Processing failed for message type '" + messageType + "': " + cause.getMessage(), cause);
        this.messageType = messageType;
    }

    public String getMessageType() {
        return messageType;
    }
}
