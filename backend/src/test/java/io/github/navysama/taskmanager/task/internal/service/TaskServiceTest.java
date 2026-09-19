package io.github.navysama.taskmanager.task.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.navysama.taskmanager.shared.error.ApiException;
import io.github.navysama.taskmanager.shared.error.ErrorCode;
import io.github.navysama.taskmanager.task.internal.dto.TaskRequests;
import io.github.navysama.taskmanager.task.internal.dto.TaskResponse;
import io.github.navysama.taskmanager.task.internal.model.Task;
import io.github.navysama.taskmanager.task.internal.model.TaskStatus;
import io.github.navysama.taskmanager.task.internal.repository.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-19T10:00:00.123456789Z");
    private static final long OWNER = 7L;

    @Mock
    private TaskRepository tasks;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(tasks, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void create_shouldDefaultToTodoAndStampMicrosecondPrecisionTimes() {
        when(tasks.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse created = taskService.create(OWNER, new TaskRequests.Create("Write tests", "  ", null));

        assertThat(created.status()).isEqualTo(TaskStatus.TODO);
        assertThat(created.description()).isNull();
        assertThat(created.createdAt()).isEqualTo(Instant.parse("2026-09-19T10:00:00.123456Z"));
        assertThat(created.updatedAt()).isEqualTo(created.createdAt());
    }

    @Test
    void update_shouldReplaceFields_whenCallerOwnsTheTask() {
        Task task = Task.create(OWNER, "Old", "old", TaskStatus.TODO, Instant.EPOCH);
        when(tasks.findByIdAndOwnerId(1L, OWNER)).thenReturn(Optional.of(task));

        TaskResponse updated = taskService.update(OWNER, 1L, new TaskRequests.Update("New", null, TaskStatus.DONE));

        assertThat(updated.title()).isEqualTo("New");
        assertThat(updated.description()).isNull();
        assertThat(updated.status()).isEqualTo(TaskStatus.DONE);
        assertThat(updated.updatedAt()).isAfter(updated.createdAt());
    }

    @Test
    void update_shouldThrowTaskNotFound_whenTaskBelongsToSomeoneElse() {
        when(tasks.findByIdAndOwnerId(1L, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(OWNER, 1L, new TaskRequests.Update("X", null, TaskStatus.DONE)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.TASK_NOT_FOUND);
    }

    @Test
    void delete_shouldNotDeleteAnything_whenTaskIsNotOwned() {
        when(tasks.findByIdAndOwnerId(1L, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(OWNER, 1L)).isInstanceOf(ApiException.class);
        verify(tasks, never()).delete(any(Task.class));
    }
}
