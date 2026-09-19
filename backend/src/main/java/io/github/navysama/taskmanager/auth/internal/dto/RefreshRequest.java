package io.github.navysama.taskmanager.auth.internal.dto;

/** Mobile-mode body of refresh and logout. The web mode sends the refresh token as an HttpOnly cookie instead. */
public record RefreshRequest(String refreshToken) {}
