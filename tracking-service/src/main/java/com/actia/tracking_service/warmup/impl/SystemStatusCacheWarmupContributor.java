package com.actia.tracking_service.warmup.impl;

import com.actia.tracking_service.common.MessageType;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.entity.TrainSystemStatus;
import com.actia.tracking_service.repository.TrainSystemStatusRepository;
import com.actia.tracking_service.service.DeduplicationService;
import com.actia.tracking_service.strategy.HashContentExtractor;
import com.actia.tracking_service.warmup.CacheWarmupContributor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CacheWarmupContributor for the {@code train-system-status} message type.
 *
 * Queries the latest persisted {@link TrainSystemStatus} per train and seeds
 * the Redis dedup cache using the same {@link HashContentExtractor} that
 * {@code TrainSystemStatusProcessorImpl} uses — guaranteeing hash consistency.
 *
 * OCP: registered automatically as a Spring {@code @Component}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemStatusCacheWarmupContributor implements CacheWarmupContributor {

    private final TrainSystemStatusRepository              statusRepository;
    private final DeduplicationService                     dedupService;
    private final HashContentExtractor<TrainSystemStatus>  hashExtractor;

    @Override
    public void warmup(Train train) {
        statusRepository.findTopByTrain_TrainIdOrderByUpdateAtDesc(train.getTrainId())
                .ifPresent(status -> {
                    String content = hashExtractor.extract(status);
                    dedupService.seedCache(
                            MessageType.TRAIN_SYSTEM_STATUS.dedupKey(),
                            String.valueOf(train.getTrainId()),
                            content);
                    log.info("Warmed system-status cache — trainId={}", train.getTrainId());
                });
    }
}
