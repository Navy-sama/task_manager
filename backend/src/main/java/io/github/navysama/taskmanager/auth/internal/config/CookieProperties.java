package io.github.navysama.taskmanager.auth.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param cookieSecure adds the {@code Secure} attribute to auth cookies; disabled only for local plain-http runs
 */
@ConfigurationProperties("app.auth")
public record CookieProperties(boolean cookieSecure) {}
