package com.actia.tracking_service.repository;

import com.actia.tracking_service.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface TrainRepository extends JpaRepository<Train, Long> {

    @Query(value = "SELECT NEXTVAL('train_id_seq')", nativeQuery = true)
    Long nextTrainId();

    @Modifying
    @Query("UPDATE Train t SET t.updateAt = :now WHERE t.trainId = :trainId")
    void touch(@Param("trainId") Long trainId, @Param("now") Instant now);
}

