package com.actia.tracking_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Externalized configuration for the train-message processing pipeline.
 *
 * <pre>
 * tracking:
 *   train-message:
 *     critical-keywords:
 *       - Failure
 *       - Brake
 *       - Emergency
 *       - Stop
 *       - Derailment
 *       - Fire
 * </pre>
 *
 * Keyword matching is case-insensitive.  Add new keywords in application.yaml
 * without touching source code.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tracking.train-message")
public class TrainMessageProperties {

    /**
     * List of keywords that flag a train message as critical.
     * A message is critical if its {@code messageName} contains any of
     * these keywords (case-insensitive substring match).
     */
    private List<String> criticalKeywords = List.of(
            "Failure", "Brake", "Emergency", "Stop", "Derailment", "Fire"
    );
}
