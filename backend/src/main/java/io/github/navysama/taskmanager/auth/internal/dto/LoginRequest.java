package io.github.navysama.taskmanager.auth.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** No password-policy rule here: login must keep working for accounts created under an older policy. */
public record LoginRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(max = 72) String password) {

    public LoginRequest {
        email = email == null ? null : email.strip();
    }
}
