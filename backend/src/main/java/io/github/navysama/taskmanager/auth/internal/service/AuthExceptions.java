package io.github.navysama.taskmanager.auth.internal.service;

import io.github.navysama.taskmanager.shared.error.ApiException;
import io.github.navysama.taskmanager.shared.error.ErrorCode;

/** Business errors of the authentication module. Messages stay generic on purpose (no account enumeration). */
public final class AuthExceptions {

    private AuthExceptions() {}

    public static ApiException emailAlreadyUsed() {
        return new ApiException(ErrorCode.EMAIL_ALREADY_USED, "An account already exists for this e-mail address.");
    }

    public static ApiException invalidCredentials() {
        return new ApiException(ErrorCode.INVALID_CREDENTIALS, "Invalid e-mail or password.");
    }

    public static ApiException invalidRefreshToken() {
        return new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid or expired session. Please sign in again.");
    }

    public static ApiException unauthenticated() {
        return new ApiException(ErrorCode.UNAUTHENTICATED, "Authentication is required.");
    }
}
