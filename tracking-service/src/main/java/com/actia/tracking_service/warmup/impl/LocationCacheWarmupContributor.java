package com.actia.tracking_service.warmup.impl;

import com.actia.tracking_service.common.MessageType;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.entity.TrainLocation;
import com.actia.tracking_service.repository.TrainLocationRepository;
import com.actia.tracking_service.service.DeduplicationService;
import com.actia.tracking_service.strategy.HashContentExtractor;
import com.actia.tracking_service.warmup.CacheWarmupContributor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CacheWarmupContributor for the {@code train-location} message type.
 *
 * Queries the latest persisted {@link TrainLocation} per train and seeds the
 * Redis dedup cache using the same {@link HashContentExtractor} that
 * {@code TrainLocationProcessorImpl} uses — guaranteeing hash consistency.
 *
 * OCP: registered automatically as a Spring {@code @Component}; no change to
 * {@link com.actia.tracking_service.warmup.CacheWarmupService} is required.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationCacheWarmupContributor implements CacheWarmupContributor {

    private final TrainLocationRepository                  locationRepository;
    private final DeduplicationService                     dedupService;
    private final HashContentExtractor<TrainLocation>      hashExtractor;

    @Override
    public void warmup(Train train) {
        locationRepository.findTopByTrain_TrainIdOrderByUpdateAtDesc(train.getTrainId())
                .ifPresent(location -> {
                    String content = hashExtractor.extract(location);
                    dedupService.seedCache(
                            MessageType.TRAIN_LOCATION.dedupKey(),
                            String.valueOf(train.getTrainId()),
                            content);
                    log.info("Warmed location cache — trainId={}", train.getTrainId());
                });
    }
}
