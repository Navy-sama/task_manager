package io.github.navysama.taskmanager.auth.internal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.navysama.taskmanager.auth.internal.service.AuthResult;

/**
 * Web mode: only {@code user} (tokens travel as HttpOnly cookies and never reach JavaScript). Mobile mode: tokens
 * are returned in the body so that the app can keep them in secure storage.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(UserResponse user, String accessToken, String refreshToken, Long expiresIn) {

    public static AuthResponse web(AuthResult result) {
        return new AuthResponse(result.user(), null, null, null);
    }

    public static AuthResponse mobile(AuthResult result) {
        return new AuthResponse(
                result.user(),
                result.tokens().accessToken(),
                result.tokens().refreshToken(),
                result.tokens().accessTokenTtl().toSeconds());
    }
}
