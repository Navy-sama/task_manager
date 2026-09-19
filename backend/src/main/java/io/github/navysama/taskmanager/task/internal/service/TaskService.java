package io.github.navysama.taskmanager.task.internal.service;

import io.github.navysama.taskmanager.shared.error.ApiException;
import io.github.navysama.taskmanager.shared.error.ErrorCode;
import io.github.navysama.taskmanager.shared.web.PageRequests;
import io.github.navysama.taskmanager.shared.web.PageResponse;
import io.github.navysama.taskmanager.task.internal.dto.TaskRequests;
import io.github.navysama.taskmanager.task.internal.dto.TaskResponse;
import io.github.navysama.taskmanager.task.internal.model.Task;
import io.github.navysama.taskmanager.task.internal.model.TaskStatus;
import io.github.navysama.taskmanager.task.internal.repository.TaskRepository;
import io.github.navysama.taskmanager.task.internal.repository.TaskSpecifications;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every operation is scoped to the caller: ownership is enforced in the queries, not checked afterwards. */
@Service
public class TaskService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));

    private final TaskRepository tasks;
    private final Clock clock;

    public TaskService(TaskRepository tasks, Clock clock) {
        this.tasks = tasks;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(long ownerId, TaskStatus status, String query, int page, int size) {
        Specification<Task> specification = TaskSpecifications.ownedBy(ownerId);
        if (status != null) {
            specification = specification.and(TaskSpecifications.hasStatus(status));
        }
        if (query != null && !query.isBlank()) {
            specification = specification.and(TaskSpecifications.matches(query));
        }
        return PageResponse.from(tasks.findAll(specification, PageRequests.of(page, size, NEWEST_FIRST))
                .map(TaskResponse::from));
    }

    @Transactional
    public TaskResponse create(long ownerId, TaskRequests.Create request) {
        TaskStatus status = request.status() == null ? TaskStatus.TODO : request.status();
        Task task = Task.create(ownerId, request.title(), request.description(), status, now());
        return TaskResponse.from(tasks.save(task));
    }

    @Transactional
    public TaskResponse update(long ownerId, long taskId, TaskRequests.Update request) {
        Task task = findOwned(ownerId, taskId);
        task.update(request.title(), request.description(), request.status(), now());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(long ownerId, long taskId) {
        tasks.delete(findOwned(ownerId, taskId));
    }

    private Task findOwned(long ownerId, long taskId) {
        return tasks.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.TASK_NOT_FOUND, "Task not found."));
    }

    /** Truncated to microseconds: the precision of a DATETIME(6) column, so stored and returned values match. */
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
