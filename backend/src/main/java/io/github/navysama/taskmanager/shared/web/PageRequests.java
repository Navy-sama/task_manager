package io.github.navysama.taskmanager.shared.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Builds bounded page requests. Out-of-range values are clamped instead of rejected: pagination is a transport
 * detail, and an unbounded {@code size} must never reach the database.
 */
public final class PageRequests {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageRequests() {}

    public static PageRequest of(int page, int size, Sort sort) {
        int boundedPage = Math.max(page, 0);
        int boundedSize = Math.clamp(size, 1, MAX_SIZE);
        return PageRequest.of(boundedPage, boundedSize, sort);
    }
}
