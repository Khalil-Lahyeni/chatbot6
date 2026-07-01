package com.actia.tracking_service.publisher;

import com.actia.tracking_service.dto.TrainAiStateDto;

/**
 * Port for publishing AI state snapshots to the {@code train-ai-events} topic.
 *
 * The message key is the train ID string so all events for the same train
 * land on the same partition and are consumed in order by the AI microservice.
 *
 * Implementations must never throw — failures must be logged and swallowed.
 */
public interface AiEventPublisher {

    void publish(String trainId, TrainAiStateDto state);
}
