package actia.monitoring.dto;

public record TrainRequest(
    String name,
    String mission,
    String baseline,
    String diversity,
    String database
) {
}
