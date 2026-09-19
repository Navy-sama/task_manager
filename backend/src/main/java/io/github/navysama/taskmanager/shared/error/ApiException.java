package io.github.navysama.taskmanager.shared.error;

import org.springframework.web.ErrorResponseException;

/**
 * Base class of every business error. Extends Spring's {@link ErrorResponseException}, so it already carries its
 * HTTP status, its Problem Details body and optional response headers (e.g. cookies to clear).
 */
public class ApiException extends ErrorResponseException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode, String detail) {
        super(errorCode.status(), Problems.of(errorCode, detail), null);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
