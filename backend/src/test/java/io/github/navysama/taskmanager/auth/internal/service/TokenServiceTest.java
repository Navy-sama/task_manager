package io.github.navysama.taskmanager.auth.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.navysama.taskmanager.auth.internal.config.JwtProperties;
import io.github.navysama.taskmanager.auth.internal.model.RefreshToken;
import io.github.navysama.taskmanager.auth.internal.repository.RefreshTokenRepository;
import io.github.navysama.taskmanager.shared.error.ApiException;
import io.github.navysama.taskmanager.shared.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.JwtEncoder;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-19T10:00:00Z");
    private static final String RAW_TOKEN = "raw-refresh-token";

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private RefreshTokenRepository refreshTokens;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        JwtProperties properties =
                new JwtProperties("unused-in-this-test", "issuer", Duration.ofMinutes(15), Duration.ofDays(7));
        tokenService = new TokenService(jwtEncoder, properties, refreshTokens, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void consume_shouldMarkTokenUsedAndReturnOwner_whenTokenIsUsable() {
        RefreshToken token = RefreshToken.issue(42L, TokenService.hash(RAW_TOKEN), NOW, NOW.plus(Duration.ofDays(7)));
        when(refreshTokens.findByTokenHash(TokenService.hash(RAW_TOKEN))).thenReturn(Optional.of(token));
        when(refreshTokens.markUsedIfUnused(any(), eq(NOW))).thenReturn(1);

        Long owner = tokenService.consume(RAW_TOKEN);

        assertThat(owner).isEqualTo(42L);
        verify(refreshTokens).markUsedIfUnused(token.getId(), NOW);
    }

    @Test
    void consume_shouldRevokeAllSessionsAndFail_whenTokenWasAlreadyUsed() {
        RefreshToken token = RefreshToken.issue(42L, TokenService.hash(RAW_TOKEN), NOW, NOW.plus(Duration.ofDays(7)));
        token.markUsed(NOW.minusSeconds(60));
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.consume(RAW_TOKEN))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokens).revokeAllForUser(42L, NOW);
        verify(refreshTokens, never()).markUsedIfUnused(any(), any());
    }

    @Test
    void consume_shouldRevokeAllSessionsAndFail_whenConditionalUpdateLosesTheRace() {
        // Two concurrent refreshes both pass the checks above on the same snapshot; the conditional update lets
        // only one of them win. The loser must be treated exactly like reuse.
        RefreshToken token = RefreshToken.issue(42L, TokenService.hash(RAW_TOKEN), NOW, NOW.plus(Duration.ofDays(7)));
        when(refreshTokens.findByTokenHash(TokenService.hash(RAW_TOKEN))).thenReturn(Optional.of(token));
        when(refreshTokens.markUsedIfUnused(any(), eq(NOW))).thenReturn(0);

        assertThatThrownBy(() -> tokenService.consume(RAW_TOKEN))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        verify(refreshTokens).revokeAllForUser(42L, NOW);
    }

    @Test
    void consume_shouldFailWithoutRevokingEverything_whenTokenIsExpired() {
        RefreshToken token = RefreshToken.issue(42L, TokenService.hash(RAW_TOKEN), NOW.minusSeconds(10), NOW);
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> tokenService.consume(RAW_TOKEN)).isInstanceOf(ApiException.class);
        verify(refreshTokens, never()).revokeAllForUser(any(), any());
        verify(refreshTokens, never()).markUsedIfUnused(any(), any());
    }

    @Test
    void consume_shouldFail_whenTokenIsUnknownOrBlank() {
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.consume(RAW_TOKEN)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> tokenService.consume(" ")).isInstanceOf(ApiException.class);
    }

    @Test
    void revoke_shouldBeNoOp_whenTokenIsMissing() {
        tokenService.revoke(null);

        verify(refreshTokens, never()).findByTokenHash(any());
    }

    @Test
    void hash_shouldBeDeterministicLowercaseSha256Hex() {
        assertThat(TokenService.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
