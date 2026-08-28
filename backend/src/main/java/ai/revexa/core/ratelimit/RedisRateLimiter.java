package ai.revexa.core.ratelimit;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Shared limiter for multi-instance deployments: INCR + EXPIRE on a per-window key. */
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final StringRedisTemplate redis;
    private final RateLimiter fallback = new InMemoryRateLimiter();

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Decision check(String key, int limit, Duration window) {
        long bucket = System.currentTimeMillis() / Math.max(1, window.toMillis());
        String redisKey = "revexa:rl:" + key + ":" + bucket;
        try {
            Long used = redis.opsForValue().increment(redisKey);
            if (used != null && used == 1L) {
                redis.expire(redisKey, window.plusSeconds(1));
            }
            int count = used == null ? 1 : used.intValue();
            return new Decision(count <= limit, limit, Math.max(0, limit - count), window.toSeconds());
        } catch (RuntimeException e) {
            // Never let a cache outage take the API down with it.
            log.warn("Redis rate limiter unavailable, falling back to in-memory: {}", e.getMessage());
            return fallback.check(key, limit, window);
        }
    }
}
