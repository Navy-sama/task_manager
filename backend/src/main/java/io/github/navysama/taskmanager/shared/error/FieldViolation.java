package io.github.navysama.taskmanager.shared.error;

/** One entry of the {@code errors} array of a {@code VALIDATION_FAILED} problem. */
public record FieldViolation(String field, String message) {}
