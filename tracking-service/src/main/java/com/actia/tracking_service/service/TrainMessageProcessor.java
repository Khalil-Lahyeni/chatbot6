package com.actia.tracking_service.service;

import com.actia.tracking_service.dto.TrainMessageDto;

/**
 * Port — processes a validated train message received from Kafka.
 *
 * Unlike {@link TrainLocationProcessor} and {@link TrainSystemStatusProcessor},
 * implementations must persist every message without deduplication.
 */
public interface TrainMessageProcessor {

    void process(TrainMessageDto dto);
}
