package com.actia.tracking_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound Kafka payload for the {@code train-configuration} topic.
 *
 * No deduplication is applied — every validated message is persisted
 * and forwarded to the train-events topic.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainConfigurationDto {

    @NotNull(message = "trainId is required")
    @Positive(message = "trainId must be a positive number")
    private Long trainId;

    private boolean visible;

    private String ccu1Ip;

    private String ccu2Ip;
}
