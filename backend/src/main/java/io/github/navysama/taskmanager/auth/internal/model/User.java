package io.github.navysama.taskmanager.auth.internal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {}

    private User(String email, String passwordHash, Instant now) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * @param email already normalized (trimmed, lower-cased)
     * @param passwordHash BCrypt hash, never the clear-text password
     */
    public static User register(String email, String passwordHash, Instant now) {
        return new User(email, passwordHash, now);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
