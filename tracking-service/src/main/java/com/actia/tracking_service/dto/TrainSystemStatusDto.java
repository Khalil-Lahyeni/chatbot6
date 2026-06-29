package com.actia.tracking_service.dto;

import com.actia.tracking_service.enums.SystemHealthStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainSystemStatusDto {

    @NotNull(message = "trainId is required")
    @Positive(message = "trainId must be a positive number")
    private Long trainId;

    @NotNull(message = "pacisStatus is required")
    private SystemHealthStatus pacisStatus;

    @NotNull(message = "cctvStatus is required")
    private SystemHealthStatus cctvStatus;

    @NotNull(message = "rearViewStatus is required")
    private SystemHealthStatus rearViewStatus;
}
