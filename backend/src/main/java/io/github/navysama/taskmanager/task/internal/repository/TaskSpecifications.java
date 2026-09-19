package io.github.navysama.taskmanager.task.internal.repository;

import io.github.navysama.taskmanager.task.internal.model.Task;
import io.github.navysama.taskmanager.task.internal.model.TaskStatus;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Composable query criteria for the task list. */
public final class TaskSpecifications {

    /**
     * Escape character for LIKE patterns. Not a backslash: MySQL also treats backslash as a string-literal escape,
     * which makes {@code ESCAPE '\'} non-portable.
     */
    static final char LIKE_ESCAPE = '!';

    private TaskSpecifications() {}

    public static Specification<Task> ownedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Case-insensitive "contains" on title or description; user wildcards are matched literally. */
    public static Specification<Task> matches(String text) {
        String pattern = containsPattern(text);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE));
    }

    static String containsPattern(String text) {
        String escaped = text.strip()
                .toLowerCase(Locale.ROOT)
                .replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
        return "%" + escaped + "%";
    }
}
