package com.actia.tracking_service.dto;

import com.actia.tracking_service.enums.SystemHealthStatus;
import com.actia.tracking_service.enums.UpdateStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Snapshot of a train's state published to the {@code train-ai-events} topic.
 *
 * All status fields use wrapper types (Boolean, Long) so that {@code null}
 * signals "not yet received" — the publisher withholds the event until every
 * field is populated (see {@link #isComplete()}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainAiStateDto {

    private Long trainId;

    private SystemHealthStatus pacisStatus;
    private SystemHealthStatus cctvStatus;
    private SystemHealthStatus rearViewStatus;
    private UpdateStatus       updateStatus;

    /** True when the train has more than one MediaDatabase entry with isActive = true. */
    private Boolean hasMultipleActiveVersion;

    /** Reflects the {@code visible} field from the latest TrainConfiguration event. */
    private Boolean ccuVisible;

    /** Count of critical TrainMessages received in the last 60 minutes. */
    private Long nbrCriticalMessage;

    /**
     * Returns true only when every field has been populated by at least one
     * inbound event.  Publication to train-ai-events is withheld until then.
     */
    public boolean isComplete() {
        return pacisStatus              != null
            && cctvStatus              != null
            && rearViewStatus          != null
            && updateStatus            != null
            && hasMultipleActiveVersion != null
            && ccuVisible              != null
            && nbrCriticalMessage      != null;
    }
}
