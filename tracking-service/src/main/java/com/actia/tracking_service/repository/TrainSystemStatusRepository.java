package com.actia.tracking_service.repository;

import com.actia.tracking_service.entity.TrainSystemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainSystemStatusRepository extends JpaRepository<TrainSystemStatus, Long> {

    /**
     * Returns the most recent system status for the given train ID.
     *
     * Accepts a plain {@code Long} instead of a {@code Train} entity — see
     * {@link TrainLocationRepository} for the rationale.
     */
    Optional<TrainSystemStatus> findTopByTrain_TrainIdOrderByUpdateAtDesc(Long trainId);
}
