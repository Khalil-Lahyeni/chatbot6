package com.actia.tracking_service.service;

import com.actia.tracking_service.dto.TrainRequest;
import com.actia.tracking_service.dto.TrainResponse;
import com.actia.tracking_service.exception.TrainAlreadyExistsException;
import com.actia.tracking_service.exception.TrainNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TrainService {

    /**
     * @throws TrainAlreadyExistsException if a train with the same trainId already exists
     */
    TrainResponse createTrain(TrainRequest request);

    /**
     * @throws TrainNotFoundException if no train exists with the given id
     */
    TrainResponse getTrainById(Long id);

    Page<TrainResponse> getAllTrains(Pageable pageable);

    /**
     * @throws TrainNotFoundException if no train exists with the given id
     */
    TrainResponse updateTrain(Long id, TrainRequest request);

    /**
     * @throws TrainNotFoundException if no train exists with the given id
     */
    void deleteTrain(Long id);
}
