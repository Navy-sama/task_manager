package io.github.navysama.taskmanager.auth.internal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The 72-character ceiling is not arbitrary: BCrypt only uses the first 72 bytes of a password, so anything longer
 * would silently be truncated.
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {

    /** Runs at deserialization, before Bean Validation: surrounding spaces must not fail the e-mail check. */
    public RegisterRequest {
        email = email == null ? null : email.strip();
    }
}
