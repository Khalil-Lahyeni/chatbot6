package com.actia.tracking_service.mapper;

import com.actia.tracking_service.dto.TrainRequest;
import com.actia.tracking_service.dto.TrainResponse;
import com.actia.tracking_service.entity.Train;
import org.springframework.stereotype.Component;

/**
 * Stateless mapper between {@link Train} entity and its request/response DTOs.
 *
 * Kept as a plain {@code @Component} (no MapStruct dependency) since the
 * mapping is trivial field-to-field with no type conversions.
 */
@Component
public class TrainMapper {

    public TrainResponse toResponse(Train train) {
        return new TrainResponse(
                train.getTrainId(),
                train.getName(),
                train.getUpdateAt(),
                train.getMission(),
                train.getBaseline(),
                train.getDiversity(),
                train.getDatabase()
        );
    }

    public Train toEntity(TrainRequest request) {
        return Train.builder()
                .trainId(request.trainId())
                .name(request.name())
                .mission(request.mission())
                .baseline(request.baseline())
                .diversity(request.diversity())
                .database(request.database())
                .build();
    }

    /**
     * Applies all mutable fields from {@code request} onto an existing
     * managed {@code entity}.  The trainId is immutable (PK) and is not
     * overwritten.
     */
    public void updateEntity(Train entity, TrainRequest request) {
        entity.setName(request.name());
        entity.setMission(request.mission());
        entity.setBaseline(request.baseline());
        entity.setDiversity(request.diversity());
        entity.setDatabase(request.database());
    }
}
