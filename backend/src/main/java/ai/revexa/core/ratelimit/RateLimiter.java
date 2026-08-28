package ai.revexa.core.ratelimit;

import java.time.Duration;

/**
 * Fixed-window counter abstraction. The in-memory implementation is fine for a single node; the
 * Redis one keeps limits correct once the API is horizontally scaled.
 */
public interface RateLimiter {

    Decision check(String key, int limit, Duration window);

    record Decision(boolean allowed, int limit, int remaining, long retryAfterSeconds) {}
}
