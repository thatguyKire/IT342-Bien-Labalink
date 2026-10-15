package edu.cit.bien.labalink.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window, per-client-IP rate limiter.
 * /api/auth/** gets a tight budget (brute force / credential stuffing);
 * the rest of /api/** a generous one (scraping / accidental loops).
 * In-memory, so limits are per instance; see DECISIONS.md (ADR-009).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final int authLimit;
    private final int apiLimit;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(int authLimit, int apiLimit) {
        this.authLimit = authLimit;
        this.apiLimit = apiLimit;
    }

    private record Window(long start, AtomicInteger count) {}

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Stripe retries webhooks and signs them; never throttle those.
        return !path.startsWith("/api/")
            || path.equals("/api/payments/webhook");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        boolean isAuth = request.getRequestURI().startsWith("/api/auth/");
        int limit = isAuth ? authLimit : apiLimit;
        String key = (isAuth ? "auth:" : "api:") + request.getRemoteAddr();
        long now = System.currentTimeMillis();
        long windowStart = now - (now % WINDOW_MILLIS);

        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(w -> w.start() < windowStart);
        }

        Window window = windows.compute(key, (k, existing) ->
            existing == null || existing.start() != windowStart
                ? new Window(windowStart, new AtomicInteger())
                : existing);

        if (window.count().incrementAndGet() > limit) {
            long retryAfter = Math.max(1,
                (windowStart + WINDOW_MILLIS - now) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"success\":false,\"message\":"
                + "\"Too many requests. Please slow down.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
