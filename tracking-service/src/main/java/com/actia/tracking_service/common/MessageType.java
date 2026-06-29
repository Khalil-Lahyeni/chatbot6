package com.actia.tracking_service.common;

/**
 * Type-safe identifier for each category of train message.
 *
 * This enum is a pure domain type marker — it does NOT carry Kafka topic names
 * (those are externalized in {@link com.actia.tracking_service.config.KafkaProperties}).
 *
 * {@link #dedupKey()} provides a stable Redis namespace derived from the enum
 * constant's own name.  It is intentionally NOT tied to the Kafka topic name so
 * that Redis keys remain stable even when topics are renamed in configuration.
 *
 * OCP: adding a new message type requires only a new constant here, plus a
 * corresponding entry in application.yaml and a new processor/consumer pair.
 */
public enum MessageType {

    TRAIN_LOCATION,
    TRAIN_SYSTEM_STATUS,
    TRAIN_EVENTS,
    TRAIN_MESSAGE;

    /**
     * Stable Redis dedup namespace for this message type.
     * Derived from the enum constant name: {@code TRAIN_LOCATION} → {@code "train-location"}.
     * Independent of the Kafka topic name configured in application.yaml.
     */
    public String dedupKey() {
        return name().toLowerCase().replace('_', '-');
    }

    @Override
    public String toString() {
        return dedupKey();
    }
}
