package actia.monitoring.service;

import actia.monitoring.dto.TrainRequest;
import actia.monitoring.dto.TrainResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TrainService {
    TrainResponse createTrain(TrainRequest request);
    TrainResponse getTrainById(Long id);
    Page<TrainResponse> getAllTrains(Pageable pageable);
    TrainResponse updateTrain(Long id, TrainRequest request);
    void deleteTrain(Long id);
}
