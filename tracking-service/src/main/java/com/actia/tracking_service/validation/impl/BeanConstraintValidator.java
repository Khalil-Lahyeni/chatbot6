package com.actia.tracking_service.validation.impl;

import com.actia.tracking_service.exception.MessageValidationException;
import com.actia.tracking_service.validation.MessageValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Jakarta Bean Validation implementation of {@link MessageValidator}.
 *
 * Renamed from {@code TrainMessageValidator}: this component is fully generic
 * ({@code <T>}) and has no coupling to the Train domain.
 *
 * Throws {@link MessageValidationException} carrying all violation messages so
 * the consumer pipeline handles the failure once, in one place, without
 * repeating logging or branching logic.
 */
@Component
@RequiredArgsConstructor
public class BeanConstraintValidator implements MessageValidator {

    private final Validator validator;

    @Override
    public <T> void validateOrThrow(T dto) throws MessageValidationException {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);

        if (violations.isEmpty()) {
            return;
        }

        List<String> messages = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();

        throw new MessageValidationException(dto.getClass().getSimpleName(), messages);
    }
}
