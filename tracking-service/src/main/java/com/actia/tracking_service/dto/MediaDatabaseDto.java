package com.actia.tracking_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Inbound Kafka payload for the {@code train-media-database} topic.
 *
 * No deduplication is applied — every validated message is persisted
 * and forwarded to the train-events topic.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaDatabaseDto {

    @NotNull(message = "trainId is required")
    @Positive(message = "trainId must be a positive number")
    private Long trainId;

    private String deviceIp;

    @NotBlank(message = "name is required")
    private String name;

    private String versionNumber;

    private Boolean isActive;

    private Instant activationDate;
}
