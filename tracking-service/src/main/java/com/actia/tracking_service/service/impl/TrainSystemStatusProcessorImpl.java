package com.actia.tracking_service.service.impl;

import com.actia.tracking_service.common.MessageType;
import com.actia.tracking_service.common.TrainResolver;
import com.actia.tracking_service.dto.TrainSystemStatusDto;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.entity.TrainSystemStatus;
import com.actia.tracking_service.enums.UpdateStatus;
import com.actia.tracking_service.publisher.EventPublisher;
import com.actia.tracking_service.repository.TrainSystemStatusRepository;
import com.actia.tracking_service.service.DeduplicationService;
import com.actia.tracking_service.service.TrainSystemStatusProcessor;
import com.actia.tracking_service.strategy.HashContentExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SRP-compliant implementation of {@link TrainSystemStatusProcessor}.
 *
 * Builder Pattern fix: replaced the previous inline setter-chain with the
 * Lombok {@code @Builder} consistently present on {@link TrainSystemStatus}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainSystemStatusProcessorImpl implements TrainSystemStatusProcessor {

    private final DeduplicationService                     dedupService;
    private final TrainResolver                            trainResolver;
    private final TrainSystemStatusRepository              statusRepository;
    private final EventPublisher                           eventPublisher;
    private final HashContentExtractor<TrainSystemStatusDto> hashExtractor;

    @Transactional
    @Override
    public void process(TrainSystemStatusDto dto) {
        String trainIdStr = String.valueOf(dto.getTrainId());
        String content    = hashExtractor.extract(dto);

        if (!dedupService.isNew(MessageType.TRAIN_SYSTEM_STATUS.dedupKey(), trainIdStr, content)) {
            return; // duplicate — DedupService already logged
        }

        Train train = trainResolver.resolveOrCreate(dto.getTrainId());

        // Builder Pattern — replaces the previous setter-chain anti-pattern
        TrainSystemStatus entity = TrainSystemStatus.builder()
                .pacisStatus(dto.getPacisStatus())
                .cctvStatus(dto.getCctvStatus())
                .rearViewStatus(dto.getRearViewStatus())
                .updateStatus(UpdateStatus.UPDATED)
                .train(train)
                .build();

        statusRepository.save(entity);
        log.info("Saved TrainSystemStatus for trainId={}", dto.getTrainId());

        eventPublisher.publish(trainIdStr, dto);
    }
}
