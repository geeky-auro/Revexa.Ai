package ai.revexa.core.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/** Default limiter: process-local, zero infrastructure, good enough for dev and single-node runs. */
public class InMemoryRateLimiter implements RateLimiter {

    private record Window(Instant resetAt, AtomicInteger count) {}

    private final Cache<String, Window> windows =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(100_000).build();

    @Override
    public Decision check(String key, int limit, Duration window) {
        Instant now = Instant.now();
        Window current =
                windows.asMap()
                        .compute(
                                key,
                                (k, existing) ->
                                        existing == null || existing.resetAt().isBefore(now)
                                                ? new Window(now.plus(window), new AtomicInteger(0))
                                                : existing);
        int used = current.count().incrementAndGet();
        long retryAfter = Math.max(0, current.resetAt().getEpochSecond() - now.getEpochSecond());
        return new Decision(used <= limit, limit, Math.max(0, limit - used), retryAfter);
    }
}
