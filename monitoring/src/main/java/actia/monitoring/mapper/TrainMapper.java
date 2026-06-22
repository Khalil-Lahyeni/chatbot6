package actia.monitoring.mapper;

import actia.monitoring.entity.Train;
import actia.monitoring.dto.TrainRequest;
import actia.monitoring.dto.TrainResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TrainMapper {

    @Mapping(source = "database", target = "database")
    Train toEntity(TrainRequest dto);

    TrainResponse toResponse(Train train);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    void updateEntityFromDto(TrainRequest dto, @MappingTarget Train entity);
}
