package com.actia.tracking_service.repository;

import com.actia.tracking_service.entity.TrainLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainLocationRepository extends JpaRepository<TrainLocation, Long> {

    /**
     * Returns the most recent location for the given train ID.
     *
     * Accepts a plain {@code Long} instead of a {@code Train} entity to avoid
     * forcing callers to load a full entity object just to query by FK.
     * Spring Data resolves {@code Train_TrainId} to the nested path
     * {@code train.trainId} automatically.
     */
    Optional<TrainLocation> findTopByTrain_TrainIdOrderByUpdateAtDesc(Long trainId);

    List<TrainLocation> findAllByTrain_TrainId(Long trainId);
}
