package com.actia.tracking_service.repository;

import com.actia.tracking_service.entity.TrainMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainMessageRepository extends JpaRepository<TrainMessage, Long> {

    List<TrainMessage> findByTrain_TrainIdOrderByCreatedAtDesc(Long trainId);

    List<TrainMessage> findByIsCriticalTrueOrderByCreatedAtDesc();
}
