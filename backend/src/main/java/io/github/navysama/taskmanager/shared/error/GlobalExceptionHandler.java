package io.github.navysama.taskmanager.shared.error;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Single place where every error becomes an RFC 9457 Problem Details response with a stable {@code code}.
 *
 * <p>Spring MVC exceptions are handled by {@link ResponseEntityExceptionHandler}; this class only adds the
 * {@code code} extension, the field errors and neutral messages. Security exceptions raised in the filter chain
 * (before the DispatcherServlet) are routed here by the security entry point and access-denied handler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(ErrorCode.UNAUTHENTICATED.status())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(Problems.of(ErrorCode.UNAUTHENTICATED, "Authentication is required."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ex instanceof CsrfException
                ? Problems.of(ErrorCode.CSRF_TOKEN_INVALID, "Missing or invalid CSRF token.")
                : Problems.of(ErrorCode.FORBIDDEN, "Access denied.");
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(Problems.of(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred."));
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = Problems.of(ErrorCode.VALIDATION_FAILED, "Request validation failed.");
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        problem.setProperty(Problems.ERRORS_PROPERTY, violations);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = Problems.of(ErrorCode.MALFORMED_REQUEST, "Malformed or unreadable request body.");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = ex.getPropertyName() == null
                ? "Invalid parameter value."
                : "Invalid value for parameter '" + ex.getPropertyName() + "'.";
        ProblemDetail problem = Problems.of(ErrorCode.INVALID_PARAMETER, detail);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ProblemDetail problem;
        if (body instanceof ProblemDetail detail) {
            problem = detail;
        } else if (ex instanceof ErrorResponse errorResponse) {
            // ErrorResponseException (incl. ApiException) is handed over with a null body: its own body is the source.
            problem = errorResponse.getBody();
        } else {
            problem = ProblemDetail.forStatus(statusCode);
        }
        if (!Problems.hasCode(problem)) {
            problem.setProperty(Problems.CODE_PROPERTY, codeFor(ex, statusCode).name());
        }
        return super.handleExceptionInternal(ex, problem, headers, statusCode, request);
    }

    private static ErrorCode codeFor(Exception ex, HttpStatusCode status) {
        if (ex instanceof NoResourceFoundException) {
            return ErrorCode.NOT_FOUND;
        }
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        }
        if (ex instanceof ServletRequestBindingException || ex instanceof HandlerMethodValidationException) {
            return ErrorCode.INVALID_PARAMETER;
        }
        return ErrorCode.forStatus(status.value());
    }
}
