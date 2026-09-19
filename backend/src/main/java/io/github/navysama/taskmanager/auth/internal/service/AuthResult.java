package io.github.navysama.taskmanager.auth.internal.service;

import io.github.navysama.taskmanager.auth.internal.dto.UserResponse;

/** Outcome of a successful register, login or refresh: who the user is and the tokens to hand out. */
public record AuthResult(UserResponse user, IssuedTokens tokens) {}
