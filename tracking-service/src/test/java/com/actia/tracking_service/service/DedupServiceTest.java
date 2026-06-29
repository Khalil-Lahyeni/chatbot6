package com.actia.tracking_service.service;

import com.actia.tracking_service.config.RedisProperties;
import com.actia.tracking_service.service.impl.RedisDedupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RedisDedupService} (implements {@link DeduplicationService}).
 *
 * Tests target the public contract (DeduplicationService interface) only.
 * Internal helpers (sha256, buildKey) are intentionally private; this test
 * uses a local {@link #sha256(String)} utility to prepare expected values
 * without coupling to implementation details.
 */
@ExtendWith(MockitoExtension.class)
class DedupServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private RedisProperties redisProperties;

    private DeduplicationService dedupService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisProperties.getDedupKeyPrefix()).thenReturn("dedup");
        dedupService = new RedisDedupService(redisTemplate, redisProperties);
    }

    // ── isNew ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isNew returns true and stores hash when no previous hash exists")
    void isNew_noExistingHash_returnsTrueAndStores() {
        when(valueOps.get(anyString())).thenReturn(null);

        boolean result = dedupService.isNew("train-location", "1", "StationA StationB DestC");

        assertThat(result).isTrue();
        verify(valueOps).set(eq("dedup:train-location:1"), anyString());
    }

    @Test
    @DisplayName("isNew returns true and updates hash when content changed")
    void isNew_differentHash_returnsTrueAndUpdates() {
        String oldHash = sha256("Old Old Old");
        when(valueOps.get("dedup:train-location:1")).thenReturn(oldHash);

        boolean result = dedupService.isNew("train-location", "1", "New New New");

        assertThat(result).isTrue();
        verify(valueOps).set(eq("dedup:train-location:1"), anyString());
    }

    @Test
    @DisplayName("isNew returns false when content is identical (duplicate)")
    void isNew_sameHash_returnsFalseAndDoesNotStore() {
        String content       = "StationA StationB DestC";
        String existingHash  = sha256(content);
        when(valueOps.get("dedup:train-location:1")).thenReturn(existingHash);

        boolean result = dedupService.isNew("train-location", "1", content);

        assertThat(result).isFalse();
        verify(valueOps, never()).set(anyString(), anyString());
    }

    @Test
    @DisplayName("isNew fails open and returns true when Redis throws an exception")
    void isNew_redisUnavailable_failsOpenReturnsTrue() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

        boolean result = dedupService.isNew("train-location", "1", "StationA StationB DestC");

        assertThat(result).isTrue();
        verify(valueOps, never()).set(anyString(), anyString());
    }

    // ── seedCache ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("seedCache unconditionally writes the hash without reading first")
    void seedCache_writesHashWithoutReading() {
        dedupService.seedCache("train-location", "42", "A B Dest");

        verify(valueOps).set(eq("dedup:train-location:42"), anyString());
        verify(valueOps, never()).get(anyString());
    }

    @Test
    @DisplayName("seedCache silently swallows Redis errors — no exception propagated")
    void seedCache_redisError_doesNotThrow() {
        doThrow(new RuntimeException("Redis down")).when(valueOps).set(anyString(), anyString());

        dedupService.seedCache("train-location", "42", "content"); // must not throw
    }

    // ── Key independence ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Different messageTypes produce independent Redis keys")
    void isNew_differentMessageTypes_independentKeys() {
        when(valueOps.get(anyString())).thenReturn(null);

        dedupService.isNew("train-location",      "1", "A B C");
        dedupService.isNew("train-system-status", "1", "A B C");

        verify(valueOps).set(eq("dedup:train-location:1"),      anyString());
        verify(valueOps).set(eq("dedup:train-system-status:1"), anyString());
    }

    // ── Local SHA-256 helper (does NOT call any RedisDedupService internal) ───

    /**
     * Computes SHA-256 independently from the implementation so tests remain
     * valid even if the internal helper is renamed or moved.
     */
    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
