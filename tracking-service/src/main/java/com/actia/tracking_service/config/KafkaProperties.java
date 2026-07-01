package com.actia.tracking_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for all Kafka-specific values.
 *
 * Bound from the {@code tracking.kafka.*} namespace in application.yaml so
 * that topic names, consumer settings, and retry behaviour can be changed per
 * environment without touching source code.
 *
 * <pre>
 * tracking:
 *   kafka:
 *     consumer:
 *       group-id: train-consumer-group
 *       concurrency: 1
 *     topics:
 *       location: train-location
 *       system-status: train-status
 *       events: train-events
 *       dlt-suffix: .DLT
 *       events-partitions: 3
 *       events-replicas: 1
 *       dlt-partitions: 1
 *       dlt-replicas: 1
 *     retry:
 *       interval-ms: 1000
 *       max-retries: 2
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tracking.kafka")
public class KafkaProperties {

    private Consumer consumer = new Consumer();
    private Topics   topics   = new Topics();
    private Retry    retry    = new Retry();

    @Getter
    @Setter
    public static class Consumer {
        /** Kafka consumer group identifier. */
        private String groupId = "train-consumer-group";
        /** Number of concurrent listener threads per @KafkaListener. */
        private int concurrency = 1;
    }

    @Getter
    @Setter
    public static class Topics {
        /** Inbound topic: raw train-location messages. */
        private String location = "train-location";
        /** Inbound topic: raw train-status messages. */
        private String systemStatus = "train-status";
        /** Outbound topic: processed domain events. */
        private String events = "train-events";
        /** Outbound topic: AI microservice state snapshots. */
        private String aiEvents = "train-ai-events";
        /** Suffix appended to source topic names to form Dead Letter Topics. */
        private String dltSuffix = ".DLT";
        /** Partition count for the events output topic. */
        private int eventsPartitions = 3;
        /** Replication factor for the events output topic. */
        private int eventsReplicas = 1;
        /** Partition count for the AI events output topic. */
        private int aiEventsPartitions = 3;
        /** Replication factor for the AI events output topic. */
        private int aiEventsReplicas = 1;
        /** Partition count for DLT topics. */
        private int dltPartitions = 1;
        /** Replication factor for DLT topics. */
        private int dltReplicas = 1;
        /** Inbound topic: raw train messages (all persisted, no dedup). */
        private String trainMessage = "train-message";
        /** Inbound topic: train configuration updates (no dedup). */
        private String configuration = "train-configuration";
        /** Inbound topic: media database updates (no dedup). */
        private String mediaDatabase = "train-media-database";
    }

    @Getter
    @Setter
    public static class Retry {
        /** Milliseconds to wait between retry attempts. */
        private long intervalMs = 1000L;
        /**
         * Number of re-attempts after the first failure (passed directly to
         * {@link org.springframework.util.backoff.FixedBackOff}).
         * Total attempts = maxRetries + 1.
         */
        private long maxRetries = 2L;
    }
}
