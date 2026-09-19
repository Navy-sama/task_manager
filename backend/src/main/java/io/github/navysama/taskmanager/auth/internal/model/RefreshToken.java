package io.github.navysama.taskmanager.auth.internal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A single-use refresh token. Only its SHA-256 hash is stored: a database leak does not leak usable tokens.
 *
 * <p>Lifecycle: active → used (rotated, exactly once) or revoked (logout, theft detection). Presenting a token that
 * was already used is the signature of a stolen token.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {}

    private RefreshToken(Long userId, String tokenHash, Instant now, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken issue(Long userId, String tokenHash, Instant now, Instant expiresAt) {
        return new RefreshToken(userId, tokenHash, now, expiresAt);
    }

    public boolean wasUsed() {
        return usedAt != null;
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            this.revokedAt = now;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
