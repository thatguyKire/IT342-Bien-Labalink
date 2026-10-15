package edu.cit.bien.labalink.security;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived, single-use codes that hand a completed Google login over to
 * the web app. The JWT itself is never put in a redirect URL, where it would
 * end up in browser history, proxy logs and Referer headers.
 */
@Component
public class OAuth2CodeStore {

    private static final Duration TTL = Duration.ofSeconds(60);

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    private record Entry(Long userId, Instant expiresAt) {}

    public String issue(Long userId) {
        Instant now = Instant.now();
        codes.values().removeIf(e -> e.expiresAt().isBefore(now));

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
        codes.put(code, new Entry(userId, now.plus(TTL)));
        return code;
    }

    public Optional<Long> consume(String code) {
        Entry entry = codes.remove(code);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }
}
