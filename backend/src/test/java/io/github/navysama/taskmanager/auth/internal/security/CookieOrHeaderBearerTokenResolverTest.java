package io.github.navysama.taskmanager.auth.internal.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

class CookieOrHeaderBearerTokenResolverTest {

    private final CookieOrHeaderBearerTokenResolver resolver = new CookieOrHeaderBearerTokenResolver();

    @Test
    void resolve_shouldPreferAuthorizationHeader_overCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token");
        request.setCookies(new Cookie(AuthCookies.ACCESS_TOKEN, "cookie-token"));

        assertThat(resolver.resolve(request)).isEqualTo("header-token");
    }

    @Test
    void resolve_shouldFallBackToAccessCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks");
        request.setCookies(new Cookie(AuthCookies.ACCESS_TOKEN, "cookie-token"));

        assertThat(resolver.resolve(request)).isEqualTo("cookie-token");
    }

    @Test
    void resolve_shouldIgnoreTokens_onCredentialEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired-token");
        request.setCookies(new Cookie(AuthCookies.ACCESS_TOKEN, "expired-cookie"));

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void resolve_shouldReturnNull_whenNoTokenIsPresent() {
        assertThat(resolver.resolve(new MockHttpServletRequest("GET", "/api/tasks")))
                .isNull();
    }
}
