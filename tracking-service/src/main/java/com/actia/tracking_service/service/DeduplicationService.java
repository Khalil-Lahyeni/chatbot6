package com.actia.tracking_service.service;

/**
 * Port (DIP) — defines the deduplication contract independently of any cache
 * technology.
 *
 * High-level modules (processors, warmup contributors) depend on this
 * abstraction; the concrete Redis implementation lives in
 * {@code service.impl.RedisDedupService} and is swappable without touching
 * the callers.
 */
public interface DeduplicationService {

    /**
     * Returns {@code true} when {@code hashableContent} represents a change
     * from the last known state for the given {@code entityId} and
     * {@code messageType}.
     *
     * <p>When new, the new hash is stored atomically.  When a duplicate, the
     * stored hash is left untouched.
     *
     * <p><strong>Fail-open contract:</strong> if the cache backend is
     * unavailable, the method must return {@code true} so that the processing
     * pipeline is never blocked by infrastructure failures.
     *
     * @param messageType    logical dedup namespace (e.g. "train-location")
     * @param entityId       unique entity identifier (e.g. trainId as string)
     * @param hashableContent the concatenated business fields to hash
     * @return {@code true} if the message is new and should be processed
     */
    boolean isNew(String messageType, String entityId, String hashableContent);

    /**
     * Unconditionally writes the hash for the given key — used by
     * {@link com.actia.tracking_service.warmup.CacheWarmupContributor}
     * implementations to pre-populate the cache from persisted state at
     * startup.
     *
     * @param messageType    logical dedup namespace
     * @param entityId       unique entity identifier
     * @param hashableContent the concatenated business fields to hash
     */
    void seedCache(String messageType, String entityId, String hashableContent);
}
