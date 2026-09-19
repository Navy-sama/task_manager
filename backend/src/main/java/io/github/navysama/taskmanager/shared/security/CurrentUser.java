package io.github.navysama.taskmanager.shared.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the authenticated user's id from the validated access token (the JWT {@code sub} claim). */
public final class CurrentUser {

    private CurrentUser() {}

    public static long id(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
