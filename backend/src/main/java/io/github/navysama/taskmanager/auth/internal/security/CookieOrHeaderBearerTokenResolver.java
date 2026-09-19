package io.github.navysama.taskmanager.auth.internal.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.web.util.WebUtils;

/**
 * Tells the resource server where the access token is: the {@code Authorization: Bearer} header (mobile) first,
 * then the HttpOnly {@code access_token} cookie (web).
 */
final class CookieOrHeaderBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (ClientRequests.isPublicAuthEndpoint(request)) {
            return null;
        }
        String fromHeader = headerResolver.resolve(request);
        if (fromHeader != null) {
            return fromHeader;
        }
        Cookie cookie = WebUtils.getCookie(request, AuthCookies.ACCESS_TOKEN);
        return cookie == null || cookie.getValue().isBlank() ? null : cookie.getValue();
    }
}
