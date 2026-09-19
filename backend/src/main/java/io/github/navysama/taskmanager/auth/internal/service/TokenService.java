package io.github.navysama.taskmanager.auth.internal.service;

import io.github.navysama.taskmanager.auth.internal.config.JwtProperties;
import io.github.navysama.taskmanager.auth.internal.model.RefreshToken;
import io.github.navysama.taskmanager.auth.internal.model.User;
import io.github.navysama.taskmanager.auth.internal.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues short-lived JWT access tokens and single-use opaque refresh tokens.
 *
 * <p>Access tokens are stateless (validated by signature and expiry only, no database hit per request). Refresh
 * tokens are random 256-bit values stored as SHA-256 hashes and rotated on every use.
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public TokenService(
            JwtEncoder jwtEncoder, JwtProperties properties, RefreshTokenRepository refreshTokens, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    /** Must run inside a transaction: persists the new refresh token. */
    public IssuedTokens issue(User user) {
        Instant now = now();
        String accessToken = encodeAccessToken(user, now);
        String refreshToken = randomToken();
        refreshTokens.save(
                RefreshToken.issue(user.getId(), hash(refreshToken), now, now.plus(properties.refreshTokenTtl())));
        return new IssuedTokens(accessToken, properties.accessTokenTtl(), refreshToken, properties.refreshTokenTtl());
    }

    /**
     * Consumes a refresh token (rotation) and returns its owner's id.
     *
     * <p>A token that was already used is the signature of theft: either the attacker or the legitimate user is
     * replaying it. Every session of that user is revoked, forcing a new login. The caller's transaction must not
     * roll back on the resulting exception, otherwise the revocation would be lost.
     */
    public Long consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw AuthExceptions.invalidRefreshToken();
        }
        RefreshToken token =
                refreshTokens.findByTokenHash(hash(rawToken)).orElseThrow(AuthExceptions::invalidRefreshToken);
        Instant now = now();
        if (token.wasUsed()) {
            log.warn("Refresh token reuse detected for user {}: revoking all sessions", token.getUserId());
            refreshTokens.revokeAllForUser(token.getUserId(), now);
            throw AuthExceptions.invalidRefreshToken();
        }
        if (!token.isUsable(now)) {
            throw AuthExceptions.invalidRefreshToken();
        }
        token.markUsed(now);
        return token.getUserId();
    }

    /** Idempotent: unknown or already revoked tokens are ignored. */
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(token -> token.revoke(now()));
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", e);
        }
    }

    private String encodeAccessToken(User user, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim("email", user.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static String randomToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Truncated to microseconds: the precision of a DATETIME(6) column, so stored and returned values match. */
    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
