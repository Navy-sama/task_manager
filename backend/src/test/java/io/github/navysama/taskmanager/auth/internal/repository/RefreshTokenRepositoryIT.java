package io.github.navysama.taskmanager.auth.internal.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.navysama.taskmanager.auth.internal.model.RefreshToken;
import io.github.navysama.taskmanager.auth.internal.model.User;
import io.github.navysama.taskmanager.support.IntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * A modifying {@code @Query} needs an active transaction to execute; each test method gets one (and its writes
 * rolled back afterwards) so the two calls below observe the same row like two requests handled inside the
 * application's own {@code @Transactional} service methods would.
 */
@Transactional
class RefreshTokenRepositoryIT extends IntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private UserRepository users;

    @Test
    void markUsedIfUnused_shouldSucceedOnlyOnce_whenTwoConcurrentRefreshesRaceOnTheSameToken() {
        User user = users.save(User.register(uniqueEmail(), "irrelevant-hash", Instant.now()));
        RefreshToken token = refreshTokens.save(RefreshToken.issue(
                user.getId(),
                UUID.randomUUID().toString(),
                Instant.now(),
                Instant.now().plusSeconds(3600)));

        // Simulates two concurrent POST /api/auth/refresh calls for the same token: only one may win.
        int firstCaller = refreshTokens.markUsedIfUnused(token.getId(), Instant.now());
        int secondCaller = refreshTokens.markUsedIfUnused(token.getId(), Instant.now());

        assertThat(firstCaller).isEqualTo(1);
        assertThat(secondCaller).isEqualTo(0);
    }

    @Test
    void markUsedIfUnused_shouldFail_whenTokenWasAlreadyRevoked() {
        User user = users.save(User.register(uniqueEmail(), "irrelevant-hash", Instant.now()));
        RefreshToken token = refreshTokens.save(RefreshToken.issue(
                user.getId(),
                UUID.randomUUID().toString(),
                Instant.now(),
                Instant.now().plusSeconds(3600)));
        refreshTokens.revokeAllForUser(user.getId(), Instant.now());

        int rowsUpdated = refreshTokens.markUsedIfUnused(token.getId(), Instant.now());

        assertThat(rowsUpdated).isEqualTo(0);
    }
}
