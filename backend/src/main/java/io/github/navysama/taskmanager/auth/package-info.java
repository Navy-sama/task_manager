/**
 * Authentication module: accounts, password hashing, JWT access tokens, rotating refresh tokens, cookies and the
 * Spring Security filter chain. Everything lives in {@code internal}; no other module depends on it.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Authentication", allowedDependencies = "shared")
package io.github.navysama.taskmanager.auth;
