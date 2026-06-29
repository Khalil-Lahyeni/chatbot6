package com.actia.tracking_service.exception;

import java.util.List;

/**
 * Thrown by {@link com.actia.tracking_service.validation.MessageValidator#validateOrThrow}
 * when a DTO fails one or more Bean Validation constraints.
 *
 * Carries the full list of violation messages so callers (and logs) have
 * actionable detail without requiring access to the original ConstraintViolation set.
 *
 * This exception should NOT be retried — a structurally invalid message will
 * fail again on every attempt.  The consumer pipeline catches it and drops the
 * message without re-enqueuing.
 */
public class MessageValidationException extends TrackingServiceException {

    private final String dtoType;
    private final List<String> violations;

    public MessageValidationException(String dtoType, List<String> violations) {
        super("Validation failed for " + dtoType + ": [" + String.join(", ", violations) + "]");
        this.dtoType    = dtoType;
        this.violations = List.copyOf(violations);
    }

    public String getDtoType() {
        return dtoType;
    }

    public List<String> getViolations() {
        return violations;
    }
}
