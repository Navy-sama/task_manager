package io.github.navysama.taskmanager.shared.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable JSON shape for paginated lists. Spring Data's {@code Page} is never serialized directly: its JSON layout
 * is an implementation detail that is not guaranteed to stay stable.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
