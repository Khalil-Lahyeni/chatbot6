package com.actia.tracking_service.service.impl;

import com.actia.tracking_service.dto.TrainAiStateDto;
import com.actia.tracking_service.dto.TrainSystemStatusDto;
import com.actia.tracking_service.enums.UpdateStatus;
import com.actia.tracking_service.publisher.AiEventPublisher;
import com.actia.tracking_service.service.TrainAiStateCache;
import com.actia.tracking_service.service.TrainAiStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * Orchestrates AI-state cache updates and conditional publication.
 *
 * Every inbound Kafka event follows the same pipeline via {@link #updateAndPublish}:
 * <ol>
 *   <li>Fetch current state from Redis (or create an empty shell).</li>
 *   <li>Apply only the fields that the current event type carries.</li>
 *   <li>Refresh {@code nbrCriticalMessage} from the sorted set (pure Redis, no DB).</li>
 *   <li>Publish to {@code train-ai-events} if and only if all fields are populated.</li>
 *   <li>Persist the updated state back to Redis.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainAiStateServiceImpl implements TrainAiStateService {

    private final TrainAiStateCache  aiStateCache;
    private final AiEventPublisher   aiEventPublisher;

    // ── Public API ─────────────────────────────────────────────────────────────

    @Override
    public void onSystemStatus(Long trainId, TrainSystemStatusDto dto) {
        updateAndPublish(trainId, state -> {
            state.setPacisStatus(dto.getPacisStatus());
            state.setCctvStatus(dto.getCctvStatus());
            state.setRearViewStatus(dto.getRearViewStatus());
            state.setUpdateStatus(UpdateStatus.UPDATED);
        });
    }

    @Override
    public void onMessage(Long trainId, Long messageId, boolean critical, Instant occurredAt) {
        updateAndPublish(trainId, state -> {
            if (critical) {
                long count = aiStateCache.addCriticalAndCount(trainId, messageId, occurredAt);
                state.setNbrCriticalMessage(count);
            }
        });
    }

    @Override
    public void onConfiguration(Long trainId, boolean visible) {
        updateAndPublish(trainId, state -> state.setCcuVisible(visible));
    }

    @Override
    public void onMediaDatabase(Long trainId, boolean hasMultipleActive) {
        updateAndPublish(trainId, state -> state.setHasMultipleActiveVersion(hasMultipleActive));
    }

    // ── Core pipeline ──────────────────────────────────────────────────────────

    /**
     * Shared pipeline: fetch → update → refresh critical count → publish if complete → save.
     */
    private void updateAndPublish(Long trainId, Consumer<TrainAiStateDto> updater) {
        TrainAiStateDto state = aiStateCache.get(trainId)
                .orElseGet(() -> emptyState(trainId));

        updater.accept(state);

        // Always refresh the 1-hour critical count from the sorted set (2 Redis cmds, no DB).
        // Only overwrite if the sorted set has already been seeded (warmup or prior message event).
        // If nbrCriticalMessage is still null (first time, no warmup data), we initialize it here.
        long criticalCount = aiStateCache.countCriticalLastHour(trainId);
        state.setNbrCriticalMessage(criticalCount);

        if (state.isComplete()) {
            aiEventPublisher.publish(String.valueOf(trainId), state);
            log.info("AI state published — trainId={}", trainId);
        } else {
            log.debug("AI state incomplete — publication withheld for trainId={}", trainId);
        }

        aiStateCache.save(trainId, state);
    }

    private TrainAiStateDto emptyState(Long trainId) {
        TrainAiStateDto state = new TrainAiStateDto();
        state.setTrainId(trainId);
        return state;
    }
}
