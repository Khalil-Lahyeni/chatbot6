package actia.monitoring.service.impl;

import actia.monitoring.entity.Train;
import actia.monitoring.exception.ResourceNotFoundException;
import actia.monitoring.mapper.TrainMapper;
import actia.monitoring.repository.TrainRepository;
import actia.monitoring.service.TrainService;
import actia.monitoring.dto.TrainRequest;
import actia.monitoring.dto.TrainResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TrainServiceImpl implements TrainService {

    private final TrainRepository repository;
    private final TrainMapper mapper;

    @Autowired
    public TrainServiceImpl(TrainRepository repository, TrainMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TrainResponse createTrain(TrainRequest request) {
        Train entity = mapper.toEntity(request);
        Train saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    public TrainResponse getTrainById(Long id) {
        Train train = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Train not found: " + id));
        return mapper.toResponse(train);
    }

    @Override
    public Page<TrainResponse> getAllTrains(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    public TrainResponse updateTrain(Long id, TrainRequest request) {
        Train existing = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Train not found: " + id));
        mapper.updateEntityFromDto(request, existing);
        Train saved = repository.save(existing);
        return mapper.toResponse(saved);
    }

    @Override
    public void deleteTrain(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Train not found: " + id);
        }
        repository.deleteById(id);
    }
}
