package com.actia.tracking_service.service.impl;

import com.actia.tracking_service.common.TrainResolver;
import com.actia.tracking_service.dto.MediaDatabaseDto;
import com.actia.tracking_service.entity.MediaDatabase;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.publisher.EventPublisher;
import com.actia.tracking_service.repository.MediaDatabaseRepository;
import com.actia.tracking_service.repository.TrainRepository;
import com.actia.tracking_service.service.MediaDatabaseProcessor;
import com.actia.tracking_service.service.TrainAiStateService;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists media database messages without deduplication,
 * then publishes a domain event to the train-events topic.
 *
 * Pipeline: resolve train → persist → publish event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaDatabaseProcessorImpl implements MediaDatabaseProcessor {

    private final TrainResolver           trainResolver;
    private final TrainRepository         trainRepository;
    private final MediaDatabaseRepository mediaRepository;
    private final EventPublisher          eventPublisher;
    private final TrainAiStateService     aiStateService;

    @Override
    @Transactional
    public void process(MediaDatabaseDto dto) {
        trainRepository.touch(dto.getTrainId(), Instant.now());

        Train train = trainResolver.resolveOrCreate(dto.getTrainId());

        MediaDatabase entity = MediaDatabase.builder()
                .deviceIp(dto.getDeviceIp())
                .name(dto.getName())
                .versionNumber(dto.getVersionNumber())
                .active(dto.getIsActive() != null && dto.getIsActive())
                .activationDate(dto.getActivationDate())
                .train(train)
                .build();

        mediaRepository.save(entity);
        log.info("MediaDatabase persisted — trainId={} name='{}' version='{}'",
                dto.getTrainId(), dto.getName(), dto.getVersionNumber());

        eventPublisher.publish(String.valueOf(dto.getTrainId()), dto);

        boolean hasMultipleActive = mediaRepository.countByTrain_TrainIdAndActiveTrue(dto.getTrainId()) > 1;
        aiStateService.onMediaDatabase(dto.getTrainId(), hasMultipleActive);
    }
}
