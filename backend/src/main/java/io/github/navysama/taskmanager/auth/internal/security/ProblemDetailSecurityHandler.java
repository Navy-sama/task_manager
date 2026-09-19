package io.github.navysama.taskmanager.auth.internal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 401/403 raised inside the security filter chain happen before the DispatcherServlet, so a
 * {@code @RestControllerAdvice} never sees them. This handler forwards them to the MVC exception resolvers, so that
 * security errors go through the same Problem Details formatting as every other error.
 */
@Component
class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    ProblemDetailSecurityHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        resolver.resolveException(request, response, null, authException);
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        resolver.resolveException(request, response, null, accessDeniedException);
    }
}
