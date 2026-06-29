package com.actia.tracking_service.publisher;

/**
 * Port (DIP) — abstracts the outbound event publication mechanism.
 *
 * Processors depend on this interface; the concrete Kafka implementation lives
 * in {@code publisher.impl.KafkaEventPublisher} and can be replaced (e.g. by
 * an in-memory publisher in tests) without touching any caller.
 */
public interface EventPublisher {

    /**
     * Publishes an event for the given train.
     *
     * <p>The {@code entityId} is used as the Kafka message key so that all
     * events belonging to the same train land on the same partition and are
     * consumed in order.
     *
     * <p>Implementations must never throw — publication failures must be
     * logged and swallowed to keep the processing pipeline unblocked.
     *
     * @param entityId string representation of the train ID (Kafka message key)
     * @param payload  any serialisable object; will be converted to JSON
     */
    void publish(String entityId, Object payload);
}
