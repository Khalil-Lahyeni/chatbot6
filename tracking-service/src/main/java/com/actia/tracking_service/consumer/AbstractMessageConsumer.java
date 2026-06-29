package com.actia.tracking_service.consumer;

import com.actia.tracking_service.exception.MessageValidationException;
import com.actia.tracking_service.validation.MessageValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Template Method Pattern — defines the invariant Kafka message processing
 * pipeline shared by all consumer implementations.
 *
 * <pre>
 *  handleMessage(raw JSON)
 *      ├── deserialize()        → DTO  or  null (logged, message dropped — no retry)
 *      ├── validateOrThrow()    → MessageValidationException caught here (logged, dropped — no retry)
 *      └── process(dto)         ← implemented by each subclass
 *                                  TrackingServiceException bubbles up → Kafka retry + DLT
 * </pre>
 *
 * The retry boundary is explicit: validation failures are dropped immediately
 * because they cannot succeed on retry.  Processing failures (transient DB or
 * network errors) bubble up so Spring Kafka's DefaultErrorHandler can retry
 * them and eventually route to the Dead Letter Topic.
 *
 * @param <T> the DTO type this consumer handles
 */
@Slf4j
public abstract class AbstractMessageConsumer<T> {

    private final ObjectMapper     objectMapper;
    private final MessageValidator validator;
    private final Class<T>         messageClass;

    protected AbstractMessageConsumer(ObjectMapper objectMapper,
                                      MessageValidator validator,
                                      Class<T> messageClass) {
        this.objectMapper  = objectMapper;
        this.validator     = validator;
        this.messageClass  = messageClass;
    }

    /**
     * Entry point called by {@code @KafkaListener} methods in subclasses.
     * The pipeline is final — subclasses customise only {@link #process}.
     *
     * @param rawMessage the raw JSON string received from Kafka
     */
    protected final void handleMessage(String rawMessage) {
        log.debug("Received {} message: {}", messageClass.getSimpleName(), rawMessage);

        T dto = deserialize(rawMessage);
        if (dto == null) return;

        try {
            validator.validateOrThrow(dto);
        } catch (MessageValidationException ex) {
            log.error("Dropping invalid {} message — {}", messageClass.getSimpleName(), ex.getMessage());
            return; // invalid message: dropping, not retrying
        }

        process(dto); // processing exceptions bubble up → retry + DLT
    }

    /**
     * Type-specific processing step — the only variation point.
     *
     * @param dto the validated, deserialized DTO
     */
    protected abstract void process(T dto);

    // ── Private helpers ───────────────────────────────────────────────────────

    private T deserialize(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, messageClass);
        } catch (JsonProcessingException ex) {
            log.error("Dropping malformed {} message — {}: {}",
                    messageClass.getSimpleName(), ex.getMessage(), rawMessage);
            return null;
        }
    }
}
