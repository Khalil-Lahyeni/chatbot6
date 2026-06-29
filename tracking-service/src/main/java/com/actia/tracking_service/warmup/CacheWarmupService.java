package com.actia.tracking_service.warmup;

import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates the Redis dedup-cache rebuild at startup.
 *
 * OCP fix: this class is now closed to modification.  It knows nothing about
 * individual message types — it only iterates over trains and delegates to
 * every registered {@link CacheWarmupContributor}.
 *
 * Adding support for a new message type (e.g. train-configuration) requires
 * only implementing {@link CacheWarmupContributor} and annotating the class
 * with {@code @Component}.  Spring's list injection collects all registered
 * contributors automatically.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupService implements ApplicationRunner {

    private final TrainRepository              trainRepository;
    /**
     * All {@link CacheWarmupContributor} beans discovered by Spring at startup.
     * The order is non-deterministic; contributors must be independent.
     */
    private final List<CacheWarmupContributor> contributors;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        log.info("Starting Redis cache warmup — {} contributor(s) registered", contributors.size());

        List<Train> trains = trainRepository.findAll();

        trains.forEach(train ->
                contributors.forEach(contributor -> contributor.warmup(train))
        );

        log.info("Cache warmup complete — {} train(s) × {} contributor(s) processed",
                trains.size(), contributors.size());
    }
}
