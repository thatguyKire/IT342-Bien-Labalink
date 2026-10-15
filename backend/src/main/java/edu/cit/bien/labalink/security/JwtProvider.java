package edu.cit.bien.labalink.security;

import edu.cit.bien.labalink.config.AppProperties;
import edu.cit.bien.labalink.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtProvider {

    static final String CLAIM_ROLE = "role";
    static final String CLAIM_TOKEN_VERSION = "tv";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration expiration;

    public JwtProvider(AppProperties properties) {
        String secret = properties.jwt().secret();
        if (secret == null
                || secret.getBytes(StandardCharsets.UTF_8).length
                    < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                "JWT_SECRET must be set and at least "
                + MIN_SECRET_BYTES + " bytes long. "
                + "Generate one with: openssl rand -base64 48");
        }
        this.signingKey = Keys.hmacShaKeyFor(
            secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.jwt().issuer();
        this.expiration = Duration.ofMinutes(
            properties.jwt().expirationMinutes());
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .issuer(issuer)
            .subject(String.valueOf(user.getId()))
            .claim(CLAIM_ROLE, user.getRole().name())
            .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiration)))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    /** Verifies signature, issuer and expiry; empty if anything is off. */
    public Optional<Claims> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(token)
                .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }
}
