package com.actia.tracking_service.validation;

import com.actia.tracking_service.exception.MessageValidationException;

/**
 * Port (DIP) — abstracts DTO validation so consumers depend on a contract,
 * not on Jakarta Validator directly.
 *
 * ISP: the interface is intentionally minimal (one method) so consumers are
 * not forced to depend on methods they do not use.
 *
 * Throws {@link MessageValidationException} on failure rather than returning
 * a boolean, eliminating the per-consumer {@code if (!validate(dto)) return;}
 * boilerplate and making the pipeline's intent explicit.
 */
public interface MessageValidator {

    /**
     * Validates {@code dto} against its declared Jakarta Bean Validation constraints.
     *
     * @param dto the object to validate; must carry Jakarta Bean Validation annotations
     * @throws MessageValidationException if one or more constraints are violated,
     *         carrying the full list of violation messages
     */
    <T> void validateOrThrow(T dto) throws MessageValidationException;
}
