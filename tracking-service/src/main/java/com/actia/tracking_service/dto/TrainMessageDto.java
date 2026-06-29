package com.actia.tracking_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound Kafka payload for the {@code train-message} topic.
 *
 * Unlike TrainLocationDto and TrainSystemStatusDto, messages from this topic
 * are never deduplicated — every message is persisted regardless of content.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainMessageDto {

    @NotNull(message = "trainId is required")
    @Positive(message = "trainId must be a positive number")
    private Long trainId;

    @NotBlank(message = "messageType is required")
    private String messageType;

    @NotBlank(message = "messageName is required")
    private String messageName;
}
