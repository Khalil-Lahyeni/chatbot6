package com.actia.tracking_service.repository;

import com.actia.tracking_service.entity.TrainConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainConfigurationRepository extends JpaRepository<TrainConfiguration, Long> {

    List<TrainConfiguration> findByTrain_TrainIdOrderByUpdateAtDesc(Long trainId);

    Optional<TrainConfiguration> findTopByTrain_TrainIdOrderByUpdateAtDesc(Long trainId);
}
