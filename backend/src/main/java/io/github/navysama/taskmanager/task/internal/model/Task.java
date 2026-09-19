package io.github.navysama.taskmanager.task.internal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Plain id, not a relation: the task module must not depend on the auth module's entities. */
    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    /**
     * Stored as VARCHAR. Without the explicit JDBC type, Hibernate expects a native MySQL ENUM column and schema
     * validation fails against the VARCHAR + CHECK constraint defined in the migration.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {}

    private Task(Long ownerId, String title, String description, TaskStatus status, Instant now) {
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Task create(Long ownerId, String title, String description, TaskStatus status, Instant now) {
        return new Task(ownerId, title, description, status, now);
    }

    public void update(String title, String description, TaskStatus status, Instant now) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
