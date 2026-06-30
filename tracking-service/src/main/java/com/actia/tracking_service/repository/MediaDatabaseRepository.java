package com.actia.tracking_service.repository;

import com.actia.tracking_service.entity.MediaDatabase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaDatabaseRepository extends JpaRepository<MediaDatabase, Long> {

    List<MediaDatabase> findByTrain_TrainIdOrderByUpdateAtDesc(Long trainId);
}
