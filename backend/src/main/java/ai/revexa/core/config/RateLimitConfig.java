package ai.revexa.core.config;

import ai.revexa.core.ratelimit.InMemoryRateLimiter;
import ai.revexa.core.ratelimit.RateLimiter;
import ai.revexa.core.ratelimit.RedisRateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RateLimitConfig {

    @Bean
    @ConditionalOnProperty(name = "revexa.cache.mode", havingValue = "redis")
    RateLimiter redisRateLimiter(StringRedisTemplate redisTemplate) {
        return new RedisRateLimiter(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    RateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }
}
