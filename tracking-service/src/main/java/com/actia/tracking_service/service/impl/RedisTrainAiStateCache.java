package com.actia.tracking_service.service.impl;

import com.actia.tracking_service.config.RedisProperties;
import com.actia.tracking_service.dto.TrainAiStateDto;
import com.actia.tracking_service.service.TrainAiStateCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Redis-backed implementation of {@link TrainAiStateCache}.
 *
 * Two key structures per train (no TTL — persistent):
 * <ul>
 *   <li>{@code ai:train:{id}}          — JSON of {@link TrainAiStateDto}</li>
 *   <li>{@code ai:train:{id}:critical} — Sorted Set; member = messageId,
 *       score = epoch-seconds of createdAt. Enables O(log N) 1-hour window
 *       counts without any database query.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTrainAiStateCache implements TrainAiStateCache {

    private static final long WINDOW_SECONDS = 3600L;
    private static final String CRITICAL_SUFFIX = ":critical";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper                  objectMapper;
    private final RedisProperties               redisProperties;

    // ── Key helpers ────────────────────────────────────────────────────────────

    private String stateKey(Long trainId) {
        return redisProperties.getAiStateKeyPrefix() + ":" + trainId;
    }

    private String criticalKey(Long trainId) {
        return stateKey(trainId) + CRITICAL_SUFFIX;
    }

    // ── State JSON store ───────────────────────────────────────────────────────

    @Override
    public Optional<TrainAiStateDto> get(Long trainId) {
        String json = redisTemplate.opsForValue().get(stateKey(trainId));
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, TrainAiStateDto.class));
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize AI state for trainId={}: {}", trainId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(Long trainId, TrainAiStateDto state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(stateKey(trainId), json);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize AI state for trainId={}: {}", trainId, ex.getMessage());
        }
    }

    // ── Critical sorted-set operations ────────────────────────────────────────

    @Override
    public long addCriticalAndCount(Long trainId, Long messageId, Instant occurredAt) {
        String key = criticalKey(trainId);
        redisTemplate.opsForZSet().add(key, String.valueOf(messageId), occurredAt.getEpochSecond());
        return trimAndCount(key);
    }

    @Override
    public long countCriticalLastHour(Long trainId) {
        return trimAndCount(criticalKey(trainId));
    }

    @Override
    public void seedCritical(Long trainId, Long messageId, Instant occurredAt) {
        redisTemplate.opsForZSet().add(
                criticalKey(trainId),
                String.valueOf(messageId),
                occurredAt.getEpochSecond());
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Removes entries older than 1 hour then returns the remaining count.
     * Two Redis commands: ZREMRANGEBYSCORE + ZCARD — no database query required.
     */
    private long trimAndCount(String key) {
        long cutoff = Instant.now().getEpochSecond() - WINDOW_SECONDS;
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff);
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0L;
    }
}
