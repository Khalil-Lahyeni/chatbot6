package com.actia.tracking_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for train-related behaviour.
 *
 * Bound from the {@code tracking.train.*} namespace in application.yaml.
 *
 * <pre>
 * tracking:
 *   train:
 *     auto-register-unknown: true   # default — creates placeholder for unknown IDs
 * </pre>
 *
 * Set {@code auto-register-unknown: false} in production when every trainId must
 * be pre-registered; unknown IDs will then raise
 * {@link com.actia.tracking_service.exception.TrainNotFoundException} and the
 * message will be retried by Kafka before reaching the Dead Letter Topic.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tracking.train")
public class TrainProperties {

    /**
     * When {@code true} (default), a train not found in the database is
     * automatically registered as a placeholder and processing continues.
     * When {@code false}, a {@link com.actia.tracking_service.exception.TrainNotFoundException}
     * is thrown, triggering Kafka retry and eventual DLT routing.
     */
    private boolean autoRegisterUnknown = true;
}
