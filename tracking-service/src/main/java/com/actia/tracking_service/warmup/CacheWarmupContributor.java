package com.actia.tracking_service.warmup;

import com.actia.tracking_service.entity.Train;

/**
 * Strategy Pattern — one contributor per message type responsible for
 * rebuilding the Redis dedup cache for a single train at startup.
 *
 * OCP fix: adding support for a new message type (e.g. train-configuration)
 * now requires only implementing this interface and annotating the class
 * with {@code @Component}.  The orchestrator ({@link CacheWarmupService})
 * automatically discovers all registered contributors via Spring injection
 * and never needs to be modified.
 */
public interface CacheWarmupContributor {

    /**
     * Seeds the dedup cache for {@code train} using the latest persisted state
     * as the baseline hash.
     *
     * <p>Implementations must be idempotent — calling this method multiple
     * times for the same train must produce the same cache state.
     *
     * @param train the train whose cache entry should be seeded
     */
    void warmup(Train train);
}
