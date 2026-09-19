package io.github.navysama.taskmanager.auth.internal.service;

import java.time.Duration;

/** A freshly issued token pair. The refresh token is the raw value: it is never stored, only its hash is. */
public record IssuedTokens(
        String accessToken, Duration accessTokenTtl, String refreshToken, Duration refreshTokenTtl) {}
