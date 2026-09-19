package io.github.navysama.taskmanager.task.internal.dto;

import io.github.navysama.taskmanager.task.internal.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request bodies of the task endpoints. Text is trimmed at deserialization, before Bean Validation runs. */
public final class TaskRequests {

    private TaskRequests() {}

    /** @param status optional, defaults to {@link TaskStatus#TODO} */
    public record Create(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            TaskStatus status) {

        public Create {
            title = strip(title);
            description = blankToNull(description);
        }
    }

    /** Full replacement (PUT semantics): an omitted description clears it. */
    public record Update(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotNull TaskStatus status) {

        public Update {
            title = strip(title);
            description = blankToNull(description);
        }
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
