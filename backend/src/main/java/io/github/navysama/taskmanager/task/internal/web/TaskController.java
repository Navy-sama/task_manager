package io.github.navysama.taskmanager.task.internal.web;

import io.github.navysama.taskmanager.shared.security.CurrentUser;
import io.github.navysama.taskmanager.shared.web.PageRequests;
import io.github.navysama.taskmanager.shared.web.PageResponse;
import io.github.navysama.taskmanager.task.internal.dto.TaskRequests;
import io.github.navysama.taskmanager.task.internal.dto.TaskResponse;
import io.github.navysama.taskmanager.task.internal.model.TaskStatus;
import io.github.navysama.taskmanager.task.internal.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks")
class TaskController {

    private final TaskService taskService;

    TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "List my tasks, newest update first, with optional status filter and text search")
    PageResponse<TaskResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PageRequests.DEFAULT_SIZE) int size) {
        return taskService.list(CurrentUser.id(jwt), status, q, page, size);
    }

    @PostMapping
    @Operation(summary = "Create a task")
    ResponseEntity<TaskResponse> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TaskRequests.Create request) {
        TaskResponse created = taskService.create(CurrentUser.id(jwt), request);
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace the editable fields of a task")
    TaskResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable long id, @Valid @RequestBody TaskRequests.Update request) {
        return taskService.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        taskService.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
