package actia.monitoring.dto;

import java.time.Instant;

public record TrainResponse(
    Long id,
    String name,
    String mission,
    String baseline,
    String diversity,
    String database,
    Instant updateAt
) {
}
