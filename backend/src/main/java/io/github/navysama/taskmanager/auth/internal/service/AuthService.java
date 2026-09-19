package io.github.navysama.taskmanager.auth.internal.service;

import io.github.navysama.taskmanager.auth.internal.dto.LoginRequest;
import io.github.navysama.taskmanager.auth.internal.dto.RegisterRequest;
import io.github.navysama.taskmanager.auth.internal.dto.UserResponse;
import io.github.navysama.taskmanager.auth.internal.model.User;
import io.github.navysama.taskmanager.auth.internal.repository.UserRepository;
import io.github.navysama.taskmanager.shared.error.ApiException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final Clock clock;

    /**
     * Hash compared against when the e-mail is unknown, so that "unknown account" and "wrong password" take the same
     * time (same mitigation as Spring Security's DaoAuthenticationProvider).
     */
    private final String timingDummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.clock = clock;
        this.timingDummyHash = passwordEncoder.encode("timing-attack-mitigation");
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw AuthExceptions.emailAlreadyUsed();
        }
        User user;
        try {
            user = users.saveAndFlush(User.register(
                    email,
                    passwordEncoder.encode(request.password()),
                    clock.instant().truncatedTo(ChronoUnit.MICROS)));
        } catch (DataIntegrityViolationException e) {
            // Two concurrent registrations with the same e-mail: the unique constraint is the real guard.
            throw AuthExceptions.emailAlreadyUsed();
        }
        return new AuthResult(UserResponse.from(user), tokens.issue(user));
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        Optional<User> found = users.findByEmail(normalize(request.email()));
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), timingDummyHash);
            throw AuthExceptions.invalidCredentials();
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw AuthExceptions.invalidCredentials();
        }
        return new AuthResult(UserResponse.from(user), tokens.issue(user));
    }

    /** No rollback on business errors: a detected token reuse must keep the revocation it just wrote. */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult refresh(String rawRefreshToken) {
        Long userId = tokens.consume(rawRefreshToken);
        User user = users.findById(userId).orElseThrow(AuthExceptions::invalidRefreshToken);
        return new AuthResult(UserResponse.from(user), tokens.issue(user));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        tokens.revoke(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse me(long userId) {
        return users.findById(userId).map(UserResponse::from).orElseThrow(AuthExceptions::unauthenticated);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
