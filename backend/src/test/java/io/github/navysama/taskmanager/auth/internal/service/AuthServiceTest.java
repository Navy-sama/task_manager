package io.github.navysama.taskmanager.auth.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.navysama.taskmanager.auth.internal.dto.LoginRequest;
import io.github.navysama.taskmanager.auth.internal.dto.RegisterRequest;
import io.github.navysama.taskmanager.auth.internal.model.User;
import io.github.navysama.taskmanager.auth.internal.repository.UserRepository;
import io.github.navysama.taskmanager.shared.error.ApiException;
import io.github.navysama.taskmanager.shared.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String DUMMY_HASH = "dummy-hash";

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokens;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("timing-attack-mitigation")).thenReturn(DUMMY_HASH);
        authService = new AuthService(
                users, passwordEncoder, tokens, Clock.fixed(Instant.parse("2026-09-19T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void register_shouldStoreNormalizedEmailAndHashedPassword() {
        when(users.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("correct-horse-42")).thenReturn("bcrypt-hash");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(new RegisterRequest("  Jane@Example.COM ", "correct-horse-42"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
    }

    @Test
    void register_shouldFailWith409_whenUniqueConstraintIsHitConcurrently() {
        when(users.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("correct-horse-42")).thenReturn("bcrypt-hash");
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("jane@example.com", "correct-horse-42")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_USED);
    }

    @Test
    void login_shouldStillRunAPasswordCheck_whenEmailIsUnknown() {
        when(users.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "whatever-42")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        verify(passwordEncoder).matches("whatever-42", DUMMY_HASH);
        verify(tokens, never()).issue(any());
    }

    @Test
    void login_shouldFail_whenPasswordDoesNotMatch() {
        User user = User.register("jane@example.com", "bcrypt-hash", Instant.EPOCH);
        when(users.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("wrong-password"), eq("bcrypt-hash"))).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong-password")))
                .isInstanceOf(ApiException.class);
        verify(tokens, never()).issue(any());
    }

    @Test
    void me_shouldFailWith401_whenUserNoLongerExists() {
        when(users.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(7L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }
}
