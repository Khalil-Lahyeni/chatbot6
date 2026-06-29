package com.actia.tracking_service.service.impl;

import com.actia.tracking_service.dto.TrainRequest;
import com.actia.tracking_service.dto.TrainResponse;
import com.actia.tracking_service.entity.Train;
import com.actia.tracking_service.exception.TrainNotFoundException;
import com.actia.tracking_service.mapper.TrainMapper;
import com.actia.tracking_service.repository.TrainRepository;
import com.actia.tracking_service.service.TrainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainServiceImpl implements TrainService {

    private final TrainRepository trainRepository;
    private final TrainMapper     trainMapper;

    @Override
    @Transactional
    public TrainResponse createTrain(TrainRequest request) {
        Train entity = trainMapper.toEntity(request);
        entity.setTrainId(trainRepository.nextTrainId());
        Train saved = trainRepository.save(entity);
        log.info("Train created — trainId={}", saved.getTrainId());
        return trainMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainResponse getTrainById(Long id) {
        return trainRepository.findById(id)
                .map(trainMapper::toResponse)
                .orElseThrow(() -> new TrainNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainResponse> getAllTrains(Pageable pageable) {
        return trainRepository.findAll(pageable).map(trainMapper::toResponse);
    }

    @Override
    @Transactional
    public TrainResponse updateTrain(Long id, TrainRequest request) {
        Train train = trainRepository.findById(id)
                .orElseThrow(() -> new TrainNotFoundException(id));
        trainMapper.updateEntity(train, request);
        Train saved = trainRepository.save(train);
        log.info("Train updated — trainId={}", saved.getTrainId());
        return trainMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTrain(Long id) {
        if (!trainRepository.existsById(id)) {
            throw new TrainNotFoundException(id);
        }
        trainRepository.deleteById(id);
        log.info("Train deleted — trainId={}", id);
    }
}
