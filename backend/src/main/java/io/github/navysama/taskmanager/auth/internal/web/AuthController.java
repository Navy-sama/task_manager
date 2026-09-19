package io.github.navysama.taskmanager.auth.internal.web;

import io.github.navysama.taskmanager.auth.internal.dto.AuthResponse;
import io.github.navysama.taskmanager.auth.internal.dto.LoginRequest;
import io.github.navysama.taskmanager.auth.internal.dto.RefreshRequest;
import io.github.navysama.taskmanager.auth.internal.dto.RegisterRequest;
import io.github.navysama.taskmanager.auth.internal.dto.UserResponse;
import io.github.navysama.taskmanager.auth.internal.security.AuthCookies;
import io.github.navysama.taskmanager.auth.internal.security.ClientRequests;
import io.github.navysama.taskmanager.auth.internal.service.AuthResult;
import io.github.navysama.taskmanager.auth.internal.service.AuthService;
import io.github.navysama.taskmanager.shared.error.ApiException;
import io.github.navysama.taskmanager.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
class AuthController {

    private static final String CLIENT_TYPE_DOC = "Send 'mobile' to receive tokens in the body instead of cookies.";

    private final AuthService authService;
    private final AuthCookies cookies;

    AuthController(AuthService authService, AuthCookies cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account and sign in")
    ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @Parameter(description = CLIENT_TYPE_DOC)
                    @RequestHeader(name = ClientRequests.CLIENT_TYPE_HEADER, required = false)
                    String clientType) {
        return signedIn(HttpStatus.CREATED, authService.register(request), clientType);
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with e-mail and password")
    ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @Parameter(description = CLIENT_TYPE_DOC)
                    @RequestHeader(name = ClientRequests.CLIENT_TYPE_HEADER, required = false)
                    String clientType) {
        return signedIn(HttpStatus.OK, authService.login(request), clientType);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token and issue a new access token")
    ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshRequest body,
            @CookieValue(name = AuthCookies.REFRESH_TOKEN, required = false) String refreshCookie,
            @Parameter(description = CLIENT_TYPE_DOC)
                    @RequestHeader(name = ClientRequests.CLIENT_TYPE_HEADER, required = false)
                    String clientType) {
        try {
            AuthResult result = authService.refresh(refreshToken(body, refreshCookie, clientType));
            return signedIn(HttpStatus.OK, result, clientType);
        } catch (ApiException e) {
            cookies.clear(e.getHeaders());
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the refresh token and clear the session cookies")
    ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshRequest body,
            @CookieValue(name = AuthCookies.REFRESH_TOKEN, required = false) String refreshCookie,
            @Parameter(description = CLIENT_TYPE_DOC)
                    @RequestHeader(name = ClientRequests.CLIENT_TYPE_HEADER, required = false)
                    String clientType) {
        authService.logout(refreshToken(body, refreshCookie, clientType));
        HttpHeaders headers = new HttpHeaders();
        cookies.clear(headers);
        return ResponseEntity.noContent().headers(headers).build();
    }

    @GetMapping("/me")
    @Operation(summary = "Current user")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(CurrentUser.id(jwt));
    }

    private ResponseEntity<AuthResponse> signedIn(HttpStatus status, AuthResult result, String clientType) {
        if (ClientRequests.isMobile(clientType)) {
            return ResponseEntity.status(status).body(AuthResponse.mobile(result));
        }
        HttpHeaders headers = new HttpHeaders();
        cookies.write(headers, result.tokens());
        return ResponseEntity.status(status).headers(headers).body(AuthResponse.web(result));
    }

    private static String refreshToken(RefreshRequest body, String refreshCookie, String clientType) {
        if (ClientRequests.isMobile(clientType)) {
            return body == null ? null : body.refreshToken();
        }
        return refreshCookie;
    }
}
