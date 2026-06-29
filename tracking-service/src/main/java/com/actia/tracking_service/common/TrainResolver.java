package com.actia.tracking_service.common;

import com.actia.tracking_service.config.TrainProperties;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.exception.TrainNotFoundException;
import com.actia.tracking_service.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * SRP extraction — owns the single responsibility of resolving a Train entity
 * from its ID.
 *
 * Behaviour when the train is not in the database is controlled by
 * {@link TrainProperties#isAutoRegisterUnknown()}:
 * <ul>
 *   <li>{@code true} (default) — a placeholder is created and persisted, so
 *       processing continues immediately.  Useful in development/staging.</li>
 *   <li>{@code false} — {@link TrainNotFoundException} is thrown.  The Kafka
 *       consumer will retry and eventually route to the Dead Letter Topic.
 *       Recommended for production where every trainId must be pre-registered.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainResolver {

    private final TrainRepository trainRepository;
    private final TrainProperties trainProperties;

    /**
     * Returns the existing {@link Train} for {@code trainId}.
     *
     * Creates a placeholder when {@link TrainProperties#isAutoRegisterUnknown()}
     * is {@code true}; throws {@link TrainNotFoundException} otherwise.
     *
     * @param trainId the train identifier from the incoming message
     * @return a managed, non-null Train entity
     * @throws TrainNotFoundException if the train is not registered and
     *                                auto-registration is disabled
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Train resolveOrCreate(Long trainId) {
        return trainRepository.findById(trainId)
                .orElseGet(() -> registerOrThrow(trainId));
    }

    private Train registerOrThrow(Long trainId) {
        if (!trainProperties.isAutoRegisterUnknown()) {
            throw new TrainNotFoundException(trainId);
        }
        log.warn("Train {} not registered — auto-creating placeholder "
                + "(set tracking.train.auto-register-unknown=false to disable)", trainId);
        return trainRepository.save(TrainFactory.createPlaceholder(trainId));
    }
}
