package com.actia.tracking_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for Redis-specific values.
 *
 * Bound from the {@code tracking.redis.*} namespace in application.yaml.
 *
 * <pre>
 * tracking:
 *   redis:
 *     dedup-key-prefix: dedup
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tracking.redis")
public class RedisProperties {

    /**
     * Prefix for all Redis deduplication keys.
     * Full key format: {@code {prefix}:{messageType}:{entityId}}.
     */
    private String dedupKeyPrefix = "dedup";
}
