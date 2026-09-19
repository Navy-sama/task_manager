package io.github.navysama.taskmanager.auth.internal.security;

import io.github.navysama.taskmanager.auth.internal.config.JwtProperties;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * HS256 signing: the API is both the issuer and the only consumer of its tokens, so a shared secret is enough (an
 * RSA key pair would only be needed if third parties had to verify tokens without being able to sign them).
 */
@Configuration
class JwtConfig {

    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return NimbusJwtEncoder.withSecretKey(signingKey(properties)).build();
    }

    /** Checks signature, algorithm, expiry (with the default clock skew) and issuer of every incoming token. */
    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static SecretKey signingKey(JwtProperties properties) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(properties.secret());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.jwt.secret must be Base64-encoded", e);
        }
        if (key.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret must decode to at least 32 bytes (256 bits)");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }
}
