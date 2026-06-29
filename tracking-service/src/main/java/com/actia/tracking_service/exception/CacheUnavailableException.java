package com.actia.tracking_service.exception;

/**
 * Signals that a cache backend (e.g. Redis) is unavailable or returned an
 * unexpected error.
 *
 * The deduplication service operates with a <em>fail-open</em> strategy: this
 * exception is caught internally, logged, and processing continues.  It is
 * exposed as a named type so other callers that require <em>fail-closed</em>
 * semantics can catch and rethrow it explicitly.
 */
public class CacheUnavailableException extends TrackingServiceException {

    private final String cacheName;
    private final String cacheKey;

    public CacheUnavailableException(String cacheName, String cacheKey, Throwable cause) {
        super("Cache '" + cacheName + "' unavailable for key '" + cacheKey + "': " + cause.getMessage(), cause);
        this.cacheName = cacheName;
        this.cacheKey  = cacheKey;
    }

    public String getCacheName() {
        return cacheName;
    }

    public String getCacheKey() {
        return cacheKey;
    }
}
