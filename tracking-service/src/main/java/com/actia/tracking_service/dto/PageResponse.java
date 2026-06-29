package com.actia.tracking_service.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable, self-owned pagination wrapper returned by all paginated endpoints.
 *
 * Replaces raw {@link Page} in controller return types so the JSON contract
 * is defined by this project, not by Spring Data internals.
 *
 * Usage:
 * <pre>
 *   return ResponseEntity.ok(PageResponse.of(service.getAllTrains(pageable)));
 * </pre>
 */
public record PageResponse<T>(
        List<T>  content,
        int      page,
        int      size,
        long     totalElements,
        int      totalPages,
        boolean  first,
        boolean  last
) {
    public static <T> PageResponse<T> of(Page<T> source) {
        return new PageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast()
        );
    }
}
