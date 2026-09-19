package io.github.navysama.taskmanager.auth.internal.security;

import io.github.navysama.taskmanager.auth.internal.config.CookieProperties;
import io.github.navysama.taskmanager.auth.internal.service.IssuedTokens;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Writes and clears the web-mode token cookies: HttpOnly (unreadable by JavaScript, so an XSS cannot steal them),
 * SameSite=Strict (never sent on cross-site requests) and path-scoped (the refresh token only travels to /api/auth).
 */
@Component
public class AuthCookies {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";

    private static final String ACCESS_TOKEN_PATH = "/api";
    private static final String REFRESH_TOKEN_PATH = "/api/auth";

    private final boolean secure;

    public AuthCookies(CookieProperties properties) {
        this.secure = properties.cookieSecure();
    }

    public void write(HttpHeaders headers, IssuedTokens tokens) {
        headers.add(
                HttpHeaders.SET_COOKIE,
                cookie(ACCESS_TOKEN, tokens.accessToken(), ACCESS_TOKEN_PATH, tokens.accessTokenTtl()));
        headers.add(
                HttpHeaders.SET_COOKIE,
                cookie(REFRESH_TOKEN, tokens.refreshToken(), REFRESH_TOKEN_PATH, tokens.refreshTokenTtl()));
    }

    public void clear(HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE, cookie(ACCESS_TOKEN, "", ACCESS_TOKEN_PATH, Duration.ZERO));
        headers.add(HttpHeaders.SET_COOKIE, cookie(REFRESH_TOKEN, "", REFRESH_TOKEN_PATH, Duration.ZERO));
    }

    private String cookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
