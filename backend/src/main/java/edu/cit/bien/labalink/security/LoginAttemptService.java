package edu.cit.bien.labalink.security;

import edu.cit.bien.labalink.config.AppProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-account lockout after repeated failed logins. Complements the per-IP
 * rate limiter, which alone cannot stop a distributed guessing attack
 * against a single account.
 */
@Component
public class LoginAttemptService {

    private final int maxFailures;
    private final Duration lockout;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    private record Attempts(int failures, Instant lockedUntil) {}

    public LoginAttemptService(AppProperties properties) {
        this.maxFailures = properties.login().maxFailedAttempts();
        this.lockout = Duration.ofMinutes(
            properties.login().lockoutMinutes());
    }

    public boolean isLocked(String email) {
        Attempts current = attempts.get(email);
        if (current == null || current.lockedUntil() == null) {
            return false;
        }
        if (Instant.now().isAfter(current.lockedUntil())) {
            attempts.remove(email);
            return false;
        }
        return true;
    }

    public void recordFailure(String email) {
        attempts.compute(email, (k, current) -> {
            int failures = current == null ? 1 : current.failures() + 1;
            Instant lockedUntil = failures >= maxFailures
                ? Instant.now().plus(lockout)
                : null;
            return new Attempts(failures, lockedUntil);
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(email);
    }

    public long lockoutMinutes() {
        return lockout.toMinutes();
    }
}
