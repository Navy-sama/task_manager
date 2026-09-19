package io.github.navysama.taskmanager.auth.internal.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.http.HttpHeaders;

/** Classifies incoming requests: web (cookies + CSRF) versus mobile/API clients (Bearer header). */
public final class ClientRequests {

    public static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    public static final String MOBILE_CLIENT = "mobile";

    static final String[] PUBLIC_AUTH_ENDPOINTS = {
        "/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout"
    };

    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(PUBLIC_AUTH_ENDPOINTS);
    private static final String BEARER_PREFIX = "Bearer ";

    private ClientRequests() {}

    public static boolean isMobile(String clientTypeHeader) {
        return MOBILE_CLIENT.equalsIgnoreCase(clientTypeHeader);
    }

    /**
     * Requests that cannot come from a cross-site browser form: a browser cannot attach these headers to a cross-site
     * request without a CORS preflight, which the API rejects. They are therefore exempt from CSRF checks.
     */
    static boolean isNonBrowserClient(HttpServletRequest request) {
        return isMobile(request.getHeader(CLIENT_TYPE_HEADER)) || hasBearerHeader(request);
    }

    /**
     * Credential endpoints must never be blocked by a stale access token (an expired cookie would otherwise turn a
     * login or refresh into a 401), so no token is resolved on them.
     */
    static boolean isPublicAuthEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return PUBLIC_AUTH_PATHS.contains(path);
    }

    private static boolean hasBearerHeader(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null && authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length());
    }
}
