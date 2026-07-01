package com.actia.tracking_service.warmup.impl;

import com.actia.tracking_service.dto.TrainAiStateDto;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.entity.TrainMessage;
import com.actia.tracking_service.enums.UpdateStatus;
import com.actia.tracking_service.repository.MediaDatabaseRepository;
import com.actia.tracking_service.repository.TrainConfigurationRepository;
import com.actia.tracking_service.repository.TrainMessageRepository;
import com.actia.tracking_service.repository.TrainSystemStatusRepository;
import com.actia.tracking_service.service.TrainAiStateCache;
import com.actia.tracking_service.warmup.CacheWarmupContributor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Seeds the Redis AI-state cache at startup from the latest persisted data.
 *
 * For each train:
 * <ol>
 *   <li>Latest {@code TrainSystemStatus} → pacis/cctv/rearView/updateStatus</li>
 *   <li>Count of active {@code MediaDatabase} entries → hasMultipleActiveVersion</li>
 *   <li>Latest {@code TrainConfiguration} → ccuVisible</li>
 *   <li>Critical {@code TrainMessage}s in last 60 min → sorted set + nbrCriticalMessage</li>
 * </ol>
 *
 * OCP: registered automatically as a {@code @Component}; no change to
 * {@link com.actia.tracking_service.warmup.CacheWarmupService} is required.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiStateCacheWarmupContributor implements CacheWarmupContributor {

    private static final long WINDOW_SECONDS = 3600L;

    private final TrainSystemStatusRepository  statusRepository;
    private final MediaDatabaseRepository      mediaRepository;
    private final TrainConfigurationRepository configRepository;
    private final TrainMessageRepository       messageRepository;
    private final TrainAiStateCache            aiStateCache;

    @Override
    public void warmup(Train train) {
        Long trainId = train.getTrainId();
        TrainAiStateDto state = new TrainAiStateDto();
        state.setTrainId(trainId);

        // 1. System status
        statusRepository.findTopByTrain_TrainIdOrderByUpdateAtDesc(trainId)
                .ifPresent(s -> {
                    state.setPacisStatus(s.getPacisStatus());
                    state.setCctvStatus(s.getCctvStatus());
                    state.setRearViewStatus(s.getRearViewStatus());
                    state.setUpdateStatus(s.getUpdateStatus() != null
                            ? s.getUpdateStatus()
                            : UpdateStatus.UPDATED);
                });

        // 2. Multiple active media-database versions
        long activeCount = mediaRepository.countByTrain_TrainIdAndActiveTrue(trainId);
        state.setHasMultipleActiveVersion(activeCount > 1);

        // 3. CCU visibility from latest configuration
        configRepository.findTopByTrain_TrainIdOrderByUpdateAtDesc(trainId)
                .ifPresent(c -> state.setCcuVisible(c.isVisible()));

        // 4. Critical messages in the last hour — seed sorted set + count
        Instant since = Instant.now().minusSeconds(WINDOW_SECONDS);
        List<TrainMessage> recentCritical =
                messageRepository.findByTrain_TrainIdAndIsCriticalTrueAndCreatedAtAfter(trainId, since);

        recentCritical.forEach(msg ->
                aiStateCache.seedCritical(trainId, msg.getMessageId(), msg.getCreatedAt()));

        state.setNbrCriticalMessage((long) recentCritical.size());

        aiStateCache.save(trainId, state);

        log.info("AI state cache warmed — trainId={} complete={}", trainId, state.isComplete());
    }
}
