package io.github.navysama.taskmanager.task.internal.dto;

import io.github.navysama.taskmanager.task.internal.model.Task;
import io.github.navysama.taskmanager.task.internal.model.TaskStatus;
import java.time.Instant;

/** Explicit allow-list of exposed fields: the owner id is deliberately not part of the API. */
public record TaskResponse(
        Long id, String title, String description, TaskStatus status, Instant createdAt, Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
