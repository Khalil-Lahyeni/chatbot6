package com.actia.tracking_service.strategy.impl;

import com.actia.tracking_service.config.TrainMessageProperties;
import com.actia.tracking_service.strategy.CriticalEventDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Keyword-based implementation of {@link CriticalEventDetector}.
 *
 * A message is critical if its name contains any keyword from
 * {@link TrainMessageProperties#getCriticalKeywords()} (case-insensitive).
 *
 * The keyword list is configurable in application.yaml under
 * {@code tracking.train-message.critical-keywords} — no code change needed
 * to add or remove keywords.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordCriticalEventDetector implements CriticalEventDetector {

    private final TrainMessageProperties properties;

    @Override
    public boolean isCritical(String messageName) {
        if (messageName == null || messageName.isBlank()) {
            return false;
        }
        String lowerName = messageName.toLowerCase();
        boolean critical = properties.getCriticalKeywords().stream()
                .anyMatch(keyword -> lowerName.contains(keyword.toLowerCase()));

        if (critical) {
            log.warn("Critical train message detected — name='{}'", messageName);
        }
        return critical;
    }
}
