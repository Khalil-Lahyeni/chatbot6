package com.actia.tracking_service.service.impl;

import com.actia.tracking_service.config.RedisProperties;
import com.actia.tracking_service.exception.CacheUnavailableException;
import com.actia.tracking_service.service.DeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Redis-backed implementation of {@link DeduplicationService}.
 *
 * Key structure : {@code {dedupKeyPrefix}:{messageType}:{entityId}}
 * The prefix is read from {@link RedisProperties#getDedupKeyPrefix()} so it
 * can be changed in application.yaml without touching this class.
 *
 * Fail-open contract: any Redis exception is wrapped in a
 * {@link CacheUnavailableException}, logged at ERROR, and the method returns
 * {@code true} so the pipeline never stalls on cache failures.
 *
 * Thread safety: {@link MessageDigest} is not thread-safe; a new instance is
 * created per call.  For higher throughput, consider a ThreadLocal pool.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDedupService implements DeduplicationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisProperties               redisProperties;

    // ── DeduplicationService ──────────────────────────────────────────────────

    @Override
    public boolean isNew(String messageType, String entityId, String hashableContent) {
        String key     = buildKey(messageType, entityId);
        String newHash = sha256(hashableContent);

        try {
            String stored = redisTemplate.opsForValue().get(key);

            if (newHash.equals(stored)) {
                log.debug("Duplicate ignored — key={}", key);
                return false;
            }

            redisTemplate.opsForValue().set(key, newHash);
            log.info("New message accepted — key={}", key);
            return true;

        } catch (Exception ex) {
            CacheUnavailableException cacheEx = new CacheUnavailableException("Redis", key, ex);
            log.error("Failing open — {}", cacheEx.getMessage());
            return true; // fail-open: processing continues despite cache failure
        }
    }

    @Override
    public void seedCache(String messageType, String entityId, String hashableContent) {
        String key  = buildKey(messageType, entityId);
        String hash = sha256(hashableContent);
        try {
            redisTemplate.opsForValue().set(key, hash);
            log.info("Cache seeded — key={}", key);
        } catch (Exception ex) {
            CacheUnavailableException cacheEx = new CacheUnavailableException("Redis", key, ex);
            log.error("Cache seed skipped — {}", cacheEx.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildKey(String messageType, String entityId) {
        return redisProperties.getDedupKeyPrefix() + ":" + messageType + ":" + entityId;
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandated by the JVM spec — this branch is unreachable
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
