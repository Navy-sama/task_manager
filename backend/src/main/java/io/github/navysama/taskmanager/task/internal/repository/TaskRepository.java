package io.github.navysama.taskmanager.task.internal.repository;

import io.github.navysama.taskmanager.task.internal.model.Task;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /** Ownership is part of the lookup itself: another user's task is indistinguishable from a missing one. */
    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);
}
