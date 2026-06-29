package com.actia.tracking_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainLocationDto {

    /** Matches train.train_id (BIGINT). The JSON key "trainId" maps here. */
    @NotNull(message = "trainId is required")
    @Positive(message = "trainId must be a positive number")
    private Long trainId;

    @NotBlank(message = "currentStation is required")
    private String currentStation;

    @NotBlank(message = "nextStation is required")
    private String nextStation;

    @NotBlank(message = "destination is required")
    private String destination;
}
