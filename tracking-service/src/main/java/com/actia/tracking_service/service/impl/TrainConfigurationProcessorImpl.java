package com.actia.tracking_service.service.impl;

import com.actia.tracking_service.common.TrainResolver;
import com.actia.tracking_service.dto.TrainConfigurationDto;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.entity.TrainConfiguration;
import com.actia.tracking_service.publisher.EventPublisher;
import com.actia.tracking_service.repository.TrainConfigurationRepository;
import com.actia.tracking_service.repository.TrainRepository;
import com.actia.tracking_service.service.TrainAiStateService;
import com.actia.tracking_service.service.TrainConfigurationProcessor;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists train configuration messages without deduplication,
 * then publishes a domain event to the train-events topic.
 *
 * Pipeline: resolve train → persist → publish event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainConfigurationProcessorImpl implements TrainConfigurationProcessor {

    private final TrainResolver                  trainResolver;
    private final TrainRepository                trainRepository;
    private final TrainConfigurationRepository   configRepository;
    private final EventPublisher                 eventPublisher;
    private final TrainAiStateService            aiStateService;

    @Override
    @Transactional
    public void process(TrainConfigurationDto dto) {
        trainRepository.touch(dto.getTrainId(), Instant.now());

        Train train = trainResolver.resolveOrCreate(dto.getTrainId());

        TrainConfiguration entity = TrainConfiguration.builder()
                .visible(dto.isVisible())
                .ccu1Ip(dto.getCcu1Ip())
                .ccu2Ip(dto.getCcu2Ip())
                .train(train)
                .build();

        configRepository.save(entity);
        log.info("TrainConfiguration persisted — trainId={} visible={}", dto.getTrainId(), dto.isVisible());

        eventPublisher.publish(String.valueOf(dto.getTrainId()), dto);
        aiStateService.onConfiguration(dto.getTrainId(), dto.isVisible());
    }
}
