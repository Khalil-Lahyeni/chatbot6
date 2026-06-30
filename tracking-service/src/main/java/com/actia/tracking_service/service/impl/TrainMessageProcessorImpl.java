package com.actia.tracking_service.service.impl;

import com.actia.tracking_service.common.TrainResolver;
import com.actia.tracking_service.dto.TrainMessageDto;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.entity.TrainMessage;
import com.actia.tracking_service.publisher.EventPublisher;
import com.actia.tracking_service.repository.TrainMessageRepository;
import com.actia.tracking_service.repository.TrainRepository;
import com.actia.tracking_service.service.TrainMessageProcessor;
import com.actia.tracking_service.strategy.CriticalEventDetector;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists every train message without deduplication.
 *
 * Pipeline:
 *  1. Resolve Train entity (auto-create placeholder if unknown)
 *  2. Detect whether the message is critical via {@link CriticalEventDetector}
 *  3. Build and persist the {@link TrainMessage} entity
 *
 * No call to {@link com.actia.tracking_service.service.DeduplicationService}
 * — the business requirement is that every message must be stored.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainMessageProcessorImpl implements TrainMessageProcessor {

    private final TrainResolver          trainResolver;
    private final TrainRepository        trainRepository;
    private final TrainMessageRepository messageRepository;
    private final CriticalEventDetector  criticalDetector;
    private final EventPublisher         eventPublisher;

    @Override
    @Transactional
    public void process(TrainMessageDto dto) {
        trainRepository.touch(dto.getTrainId(), Instant.now());

        Train train = trainResolver.resolveOrCreate(dto.getTrainId());

        boolean critical = criticalDetector.isCritical(dto.getMessageName());

        TrainMessage entity = TrainMessage.builder()
                .messageType(dto.getMessageType())
                .name(dto.getMessageName())
                .isCritical(critical)
                .train(train)
                .build();

        messageRepository.save(entity);
        log.info("TrainMessage persisted — trainId={} type='{}' name='{}' critical={}",
                dto.getTrainId(), dto.getMessageType(), dto.getMessageName(), critical);

        eventPublisher.publish(String.valueOf(dto.getTrainId()), dto);
    }
}
