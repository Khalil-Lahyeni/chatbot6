package com.actia.tracking_service.exception;

/**
 * Abstract root of the tracking-service exception hierarchy.
 *
 * All domain and infrastructure exceptions extend this class so callers can
 * catch the entire family with a single {@code catch (TrackingServiceException e)}
 * or selectively catch subtypes for fine-grained handling.
 *
 * All subclasses are unchecked (extends {@link RuntimeException}) so they
 * propagate freely through the Kafka listener pipeline and are handled by
 * the configured {@link org.springframework.kafka.listener.DefaultErrorHandler}.
 */
public abstract class TrackingServiceException extends RuntimeException {

    protected TrackingServiceException(String message) {
        super(message);
    }

    protected TrackingServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
