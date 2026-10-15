package edu.cit.bien.labalink.security;

import edu.cit.bien.labalink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * Turns a bearer token into an Authentication. Shared by the HTTP filter and
 * the STOMP CONNECT interceptor so both channels enforce the same rules:
 * valid signature, existing + active user, and matching token version
 * (which is how logout revokes tokens server-side).
 */
@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<UsernamePasswordAuthenticationToken> authenticate(
            String token) {
        return jwtProvider.parse(token).flatMap(claims -> {
            Long userId;
            try {
                userId = Long.valueOf(claims.getSubject());
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
            Integer tokenVersion = claims.get(
                JwtProvider.CLAIM_TOKEN_VERSION, Integer.class);

            return userRepository.findById(userId)
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .filter(user -> tokenVersion != null
                    && tokenVersion.equals(user.getTokenVersion()))
                .map(user -> {
                    UserPrincipal principal = UserPrincipal.from(user);
                    return new UsernamePasswordAuthenticationToken(
                        principal, null, principal.authorities());
                });
        });
    }

    public static String extractBearer(String headerValue) {
        if (headerValue == null
                || !headerValue.regionMatches(
                    true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        return headerValue.substring(BEARER_PREFIX.length()).trim();
    }
}
