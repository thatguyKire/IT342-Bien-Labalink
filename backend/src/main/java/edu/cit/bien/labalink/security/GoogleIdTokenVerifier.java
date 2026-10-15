package edu.cit.bien.labalink.security;

import edu.cit.bien.labalink.config.AppProperties;
import edu.cit.bien.labalink.exception.ApiException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verifies Google ID tokens sent by the Android app: signature against
 * Google's published keys, issuer, audience (our client IDs), expiry and
 * email_verified. Replaces the old endpoint that trusted whatever email
 * the client sent, which allowed logging in as anyone.
 */
@Component
public class GoogleIdTokenVerifier {

    private static final String GOOGLE_JWKS_URI =
        "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
        "accounts.google.com", "https://accounts.google.com");

    private final Set<String> clientIds;
    private final JwtDecoder decoder;

    public record GoogleUser(String email, String name) {}

    public GoogleIdTokenVerifier(AppProperties properties) {
        List<String> configured = properties.google().clientIds();
        this.clientIds = configured == null ? Set.of()
            : configured.stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty()
                    && !id.equals("not-configured"))
                .collect(Collectors.toUnmodifiableSet());

        NimbusJwtDecoder nimbus = NimbusJwtDecoder
            .withJwkSetUri(GOOGLE_JWKS_URI)
            .build();
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefault(),
            issuerValidator(),
            audienceValidator()));
        this.decoder = nimbus;
    }

    public GoogleUser verify(String idToken) {
        if (clientIds.isEmpty()) {
            throw ApiException.unavailable(
                "Google sign-in is not configured on the server");
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw ApiException.unauthorized("Invalid Google sign-in");
        }

        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
        if (email == null || email.isBlank()
                || !Boolean.TRUE.equals(emailVerified)) {
            throw ApiException.unauthorized(
                "Your Google email address is not verified");
        }
        return new GoogleUser(email, jwt.getClaimAsString("name"));
    }

    private static OAuth2TokenValidator<Jwt> issuerValidator() {
        return jwt -> jwt.getIssuer() != null
                && GOOGLE_ISSUERS.contains(jwt.getIssuer().toString())
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Bad issuer", null));
    }

    private OAuth2TokenValidator<Jwt> audienceValidator() {
        return jwt -> jwt.getAudience() != null
                && jwt.getAudience().stream().anyMatch(clientIds::contains)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Bad audience", null));
    }
}
