package io.github.navysama.taskmanager.auth.internal.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Token settings, validated at start-up: the application refuses to boot with a missing secret.
 *
 * @param secret Base64-encoded HMAC-SHA256 key (at least 32 bytes once decoded)
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl) {}
