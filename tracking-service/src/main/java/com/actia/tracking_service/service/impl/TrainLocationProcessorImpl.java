package com.actia.tracking_service.service.impl;

import com.actia.tracking_service.common.MessageType;
import com.actia.tracking_service.common.TrainResolver;
import com.actia.tracking_service.dto.TrainLocationDto;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.entity.TrainLocation;
import com.actia.tracking_service.publisher.EventPublisher;
import com.actia.tracking_service.repository.TrainLocationRepository;
import com.actia.tracking_service.service.DeduplicationService;
import com.actia.tracking_service.service.TrainLocationProcessor;
import com.actia.tracking_service.strategy.HashContentExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SRP-compliant implementation of {@link TrainLocationProcessor}.
 *
 * This class orchestrates the processing pipeline:
 * dedup → resolve train → map → persist → publish.
 * Each step is delegated to a focused collaborator; this class contains
 * no business logic of its own.
 *
 * Dependencies are all injected as interfaces (DIP), enabling independent
 * testing of each step via mocks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainLocationProcessorImpl implements TrainLocationProcessor {

    private final DeduplicationService               dedupService;
    private final TrainResolver                      trainResolver;
    private final TrainLocationRepository            locationRepository;
    private final EventPublisher                     eventPublisher;
    private final HashContentExtractor<TrainLocationDto> hashExtractor;

    @Transactional
    @Override
    public void process(TrainLocationDto dto) {
        String trainIdStr = String.valueOf(dto.getTrainId());
        String content    = hashExtractor.extract(dto);

        if (!dedupService.isNew(MessageType.TRAIN_LOCATION.dedupKey(), trainIdStr, content)) {
            return; // duplicate — DedupService already logged
        }

        Train train = trainResolver.resolveOrCreate(dto.getTrainId());

        // Builder Pattern — consistent with TrainLocation's Lombok @Builder
        TrainLocation entity = TrainLocation.builder()
                .currentStation(dto.getCurrentStation())
                .nextStation(dto.getNextStation())
                .destination(dto.getDestination())
                .train(train)
                .build();

        locationRepository.save(entity);
        log.info("Saved TrainLocation for trainId={}", dto.getTrainId());

        eventPublisher.publish(trainIdStr, dto);
    }
}
