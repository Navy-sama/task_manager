package io.github.navysama.taskmanager.auth.internal.dto;

import io.github.navysama.taskmanager.auth.internal.model.User;

public record UserResponse(Long id, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail());
    }
}
